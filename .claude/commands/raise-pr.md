현재 작업 내용에서 **(브랜치 준비) → 구현 → 커밋 → 푸시 → PR → CI 초록 확인** 플로우를 자동으로 돌려줘.

전제: 이슈는 선택. 브랜치는 이 스킬이 필요 시 생성해줌.

요청/이슈번호가 `$ARGUMENTS` 로 전달되면 사용. 없으면 아래 0단계 로직으로 자동 탐지.

---

**0단계 — 사전 확인**

병렬로 실행:
- `git status` (변경 유무 + 브랜치 상태)
- `git branch --show-current`
- 베이스 브랜치 감지: `git rev-parse --abbrev-ref origin/HEAD 2>/dev/null` → 실패 시 main/master/develop 순으로 존재 확인
- `gh auth status`
- 프로젝트 루트의 `CLAUDE.md` 가 있으면 Read — 팀 규약 (커밋 prefix, 파일 수 제한, TDD 정책 등) 수집

멈춤 조건:
- `gh` 미인증 → "gh auth login 먼저 실행하세요" 안내 후 종료

이슈 번호 결정 (우선순위, **없어도 진행**):
1. `$ARGUMENTS` 가 순수 숫자 → 그걸 `#N` 으로
2. `$ARGUMENTS` 가 `#123` 형태로 섞여 있으면 숫자 부분 추출
3. 현재 브랜치명에서 추출 (예: `feat/signup-7` → `7`)
4. `gh pr list --head <branch> --json number --jq '.[0].number'` 로 기존 PR 의 이슈 링크 확인
5. 전부 실패 → **이슈 없이 진행** (PR body 에서 `Closes #N` 생략, 커밋 메시지에서 `(#N)` 생략). AskUserQuestion 으로 막지 말 것.

---

**0.5단계 — 브랜치 준비**

현재 브랜치가 **베이스 브랜치와 동일** 하면 (main/master/develop):
- 작업 주제를 `$ARGUMENTS` 에서 추출하거나 변경 파일/커밋 메시지로부터 추론해 **새 브랜치 생성**
- 브랜치 이름 규칙: `<type>/<short-kebab>` (예: `chore/skill-addition`, `feat/signup-form`)
- 타입은 변경 성격에 따라 `feat` / `fix` / `refactor` / `test` / `docs` / `chore` 중 선택
- 생성 전 베이스 최신화:
  ```bash
  git checkout <base>
  git pull --ff-only
  git checkout -b <type>/<short-kebab>
  ```
- uncommitted 변경은 `git checkout -b` 로 그대로 따라옴 (stash 불필요)

이미 feature 브랜치에 있으면 이 단계 스킵.

---

**1단계 — 구현 & 커밋 (변경 있을 때만)**

`git status` 결과에 staged/unstaged 변경이 **있으면** 아래 순서:

**로컬 검증 먼저**: 프로젝트 스택에 맞게 실행
- Gradle: `./gradlew test` (jacoco 설정 있으면 `./gradlew test jacocoTestReport`)
- npm/pnpm/yarn: `npm test` / `pnpm test`
- Python: `pytest`
테스트 빨강이면 원인 리포트 후 멈춤 (커밋 안 함).

**커밋 분리 규칙**:
- CLAUDE.md 에 TDD 규약 있으면 **RED → GREEN → REFACTOR 단계로 커밋 분리**:
  - `test(#N): RED — <시나리오>` — 실패 테스트 + edge case
  - `feat(#N)` / `chore(#N)`: GREEN — 최소 구현
  - `refactor(#N): <개선>` (선택)
- 규약 없거나 설정/문서성 변경이면 **단일 커밋** (`chore(#N): ...` / `docs(#N): ...`)
- 커밋 타입: `feat` / `fix` / `refactor` / `test` / `docs` / `chore` 중 선택

**파일 수 제한 체크**: CLAUDE.md 에 "이슈당 N 파일 이하" 규약 있으면 현재 변경 파일 수와 비교. 초과하면 즉시 멈추고 쪼개기 제안.

**커밋 메시지 포맷** (HEREDOC 사용). 이슈 번호 있으면 `(#N)` 포함, 없으면 생략:
```
<type>(#N): <한국어 제목>         # 이슈 있을 때
<type>: <한국어 제목>             # 이슈 없을 때

- [본문 상세 bullet]

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
```

변경이 **없으면** 이 단계 스킵 (이미 커밋된 상태로 간주하고 2단계로).

---

**2단계 — 푸시**

```bash
git push -u origin <branch>
```

이미 추적 중인 브랜치면 `git push` 만. 실패 시 에러 메시지 보여주고 멈춤 (force push 금지).

---

**3단계 — PR 생성**

먼저 기존 PR 존재 여부 확인:
```bash
gh pr view <branch> --json number,state 2>/dev/null
```

- 이미 open PR 이 있으면 → "PR #M 이 이미 존재합니다. 업데이트만 할까요?" 확인 후 종료 (이 스킬은 신규 생성 전용)
- 없으면 진행

`gh pr create` 로 PR 생성. body 는 HEREDOC, 아래 구조로 한국어 작성:

```
Closes #N      # 이슈 번호 있을 때만 (없으면 이 줄 통째로 생략)

## 요약
- [핵심 변경 1~3 bullet]

## 변경 파일 (N)
| 파일 | 변경 |
|---|---|
| ... | ... |

## 검증
- [x] 로컬 테스트 초록
- [x] [기타 검증]

## Test plan
- [ ] CI 통과
- [ ] [리뷰어가 수동 확인할 항목]

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

Title 은 변경 내용 기반 (예: `chore(#18): JaCoCo + Codecov 연동`). 이슈 제목이 있으면 그것 기반으로 일관성 유지.

**UI 가 포함된 PR 이면** 사용자 메모리의 `feedback_ui_pr_screenshots.md` 규약을 따라 **스크린샷을 직접 찍어 첨부**. Playwright `page.screenshot()` 또는 `mcp__claude-in-chrome` 로 캡처 → `gh pr edit <PR> --body` 로 업데이트.

PR URL 출력.

---

**4단계 — CI 감시**

```bash
gh pr checks <PR번호>
```

- 전부 초록 → 5단계로
- 진행 중 → 사용자 대화 블록하지 말고 `ScheduleWakeup` 또는 Bash `run_in_background` 로 60~120 초 간격 재확인. `ScheduleWakeup` 권장 (delay 120s).
- 빨강 발견 → `gh run view <runId> --log-failed` 로 원인 파악 → **수정 제안** → 사용자 승인 후 1단계로 루프 (추가 커밋 → 푸시 → 재확인)

---

**5단계 — 최종 리포트 & 승인 대기**

CI 전부 초록이면 한 번에 출력:
- PR URL
- 커밋 수 (`git log <base>..HEAD --oneline | wc -l`)
- 변경 파일 수 (`git diff <base>..HEAD --stat`)
- 커버리지 변화 (설정돼 있으면 Codecov 뱃지/리포트 참고)
- CI check 목록 요약

**자동 머지 절대 금지**. "머지 진행할까요?" 로 사용자 명시 승인 받은 뒤에만:
```bash
gh pr merge <PR번호> --squash --delete-branch   # 또는 팀 관례 전략
git checkout <base> && git pull --ff-only       # 로컬 동기화
```

승인 안 주면 그냥 종료 (사용자가 직접 리뷰/머지).

---

**전반 규칙**

- 모든 출력/메시지는 한국어
- 각 단계 `TaskCreate` / `TaskUpdate` 로 추적 (in_progress → completed)
- 파괴적 명령 (`git push --force`, `git reset --hard`, 브랜치 삭제) 은 사용자 명시 승인 후에만
- `--no-verify`, `--no-gpg-sign` 등 hook/signing 우회 플래그 금지
- 커밋 메시지 / PR body 는 항상 HEREDOC 으로 전달
- 병렬 가능한 Bash 호출은 단일 메시지에서 동시 실행

---

**요청**: `$ARGUMENTS`
