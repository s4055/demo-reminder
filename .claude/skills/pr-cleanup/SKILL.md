---
name: pr-cleanup
description: PR이 머지되었는지 확인한 뒤 master로 checkout·pull하고, 해당 PR의 로컬·원격 브랜치를 삭제한다. "PR 정리해줘", "PR 머지됐으면 브랜치 정리", "/pr-cleanup" 같은 요청에 사용한다. 인자로 PR 번호를 받을 수 있다.
---

# PR 머지 후 정리

PR이 **머지된 경우에만** 아래를 수행한다. 머지되지 않았으면 상태만 알려주고 아무것도 변경하지 않는다.

## 1. 대상 PR 결정

- 인자로 PR 번호가 주어지면 그 PR을 사용한다.
- 없으면 현재 브랜치의 PR을 찾는다: `gh pr view --json number,headRefName,state,mergedAt,mergeCommit,headRefOid`
- 현재 브랜치가 `master`라서 PR을 찾을 수 없으면, 로컬에 남아 있는 브랜치 중 PR이 있는 것을 `gh pr list --state merged --json number,headRefName` 결과와 대조해 후보를 찾는다. 후보가 여러 개면 사용자에게 어떤 것을 정리할지 묻는다.

## 2. 머지 여부 확인

```bash
gh pr view <번호> --json state,mergedAt,mergeCommit,headRefName,headRefOid
```

- `state`가 `MERGED`가 아니면 여기서 멈추고 현재 상태(OPEN/CLOSED)를 알려준다.
- `CLOSED`(머지 없이 닫힘)인 경우 브랜치를 지우지 않는다. 사용자가 명시적으로 원할 때만 삭제한다.

## 3. 작업 트리 확인

- `git status --short`로 커밋되지 않은 변경이 있는지 확인한다.
- 정리 대상 브랜치에 커밋되지 않은 변경이 있으면 멈추고 사용자에게 알린다. 다른 파일의 변경은 checkout에 영향이 없으면 그대로 두고 결과 보고에 언급한다.

## 4. master checkout 및 pull

```bash
git switch master
git pull --ff-only
```

`--ff-only`가 실패하면(로컬 master가 갈라진 경우) 멈추고 사용자에게 알린다. 강제로 reset하지 않는다.

## 5. 로컬 브랜치 삭제

- 먼저 `git branch -d <headRefName>`을 시도한다.
- squash/rebase 머지라서 `-d`가 거부되면, 로컬 브랜치 끝 커밋이 PR의 `headRefOid`와 같을 때만 `git branch -D`로 삭제한다. 다르면(PR 이후 로컬에 추가 커밋이 있으면) 삭제하지 않고 사용자에게 알린다.
- 로컬 브랜치가 이미 없으면 건너뛴다.

## 6. 원격 브랜치 삭제

```bash
git ls-remote --heads origin <headRefName>   # 존재 여부 확인
git push origin --delete <headRefName>        # 있을 때만
git fetch --prune
```

GitHub 설정으로 이미 삭제되었으면 건너뛴다.

## 7. 결과 보고 (한국어)

- 머지 시각과 머지 커밋, pull 후 master의 최신 커밋
- 삭제한 로컬/원격 브랜치 (건너뛴 항목은 이유와 함께)
- `git branch -a` 결과와 남아 있는 커밋되지 않은 변경
