"use client"

import { useState } from "react"
import { useForm } from "react-hook-form"
import { LogOutIcon, UserPlusIcon, XIcon } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { useMe } from "@/hooks/use-auth"
import {
  useInviteListMember,
  useListMembers,
  useRemoveListMember,
} from "@/hooks/use-lists"
import { ApiError } from "@/lib/api"
import { isOwner, type ReminderList } from "@/lib/lists-api"

/**
 * 리스트 공유 다이얼로그. 멤버 목록은 누구나 보고, 소유자는 이메일로 초대하거나 멤버를 제거하며,
 * 편집자는 리스트에서 나갈 수 있다.
 */
export function ListShareDialog({
  list,
  onOpenChange,
  onLeft,
}: {
  list: ReminderList | null
  onOpenChange: (open: boolean) => void
  onLeft?: (list: ReminderList) => void
}) {
  // 닫힘 애니메이션 동안 내용이 비지 않도록 마지막으로 열었던 리스트를 유지한다.
  const [shownList, setShownList] = useState(list)
  if (list && list !== shownList) setShownList(list)

  return (
    <Dialog open={list !== null} onOpenChange={onOpenChange}>
      <DialogContent>
        {shownList && (
          <ShareContent
            key={shownList.id}
            list={shownList}
            onLeft={() => {
              onOpenChange(false)
              onLeft?.(shownList)
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function ShareContent({
  list,
  onLeft,
}: {
  list: ReminderList
  onLeft: () => void
}) {
  const owner = isOwner(list)
  const { data: me } = useMe()
  const { data: members, isLoading, isError, refetch } = useListMembers(list.id)
  const removeMember = useRemoveListMember()
  const [confirmLeave, setConfirmLeave] = useState(false)
  const ownerMember = members?.find((member) => member.role === "OWNER")

  return (
    <div className="grid gap-4">
      <DialogHeader>
        <DialogTitle>&ldquo;{list.name}&rdquo; 공유</DialogTitle>
        <DialogDescription>
          {owner
            ? "가입한 사용자를 이메일로 초대하면 이 리스트의 리마인더를 함께 관리할 수 있습니다."
            : ownerMember
              ? `${ownerMember.name}님이 공유한 리스트입니다. 리마인더는 함께 관리할 수 있고, 리스트 편집과 멤버 관리는 소유자만 할 수 있습니다.`
              : "공유받은 리스트입니다."}
        </DialogDescription>
      </DialogHeader>

      {owner && <InviteForm listId={list.id} />}

      <section className="grid gap-1.5" aria-label="멤버">
        <h3 className="text-xs font-semibold text-muted-foreground">
          멤버{members ? ` ${members.length}명` : ""}
        </h3>
        {isLoading && (
          <div className="grid gap-1.5" aria-label="멤버 불러오는 중">
            <Skeleton className="h-9 w-full" />
            <Skeleton className="h-9 w-full" />
          </div>
        )}
        {isError && (
          <div className="flex items-center gap-2">
            <p className="text-sm text-destructive">멤버를 불러오지 못했습니다.</p>
            <Button variant="outline" size="xs" onClick={() => refetch()}>
              다시 시도
            </Button>
          </div>
        )}
        <ul className="grid max-h-64 gap-0.5 overflow-y-auto">
          {members?.map((member) => {
            const isMe = member.userId === me?.id
            return (
              <li
                key={member.userId}
                className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-muted"
              >
                <div className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate text-sm font-medium">
                    {member.name}
                    {isMe && (
                      <span className="font-normal text-muted-foreground"> (나)</span>
                    )}
                  </span>
                  <span className="truncate text-xs text-muted-foreground">
                    {member.email}
                  </span>
                </div>
                <Badge variant={member.role === "OWNER" ? "default" : "secondary"}>
                  {member.role === "OWNER" ? "소유자" : "편집자"}
                </Badge>
                {owner && member.role !== "OWNER" && (
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    disabled={removeMember.isPending}
                    onClick={() =>
                      removeMember.mutate({ listId: list.id, userId: member.userId })
                    }
                    aria-label={`${member.name} 멤버 제거`}
                    title="멤버 제거"
                  >
                    <XIcon />
                  </Button>
                )}
              </li>
            )
          })}
        </ul>
      </section>

      {!owner && me && (
        <DialogFooter className="items-center sm:justify-between">
          {confirmLeave ? (
            <>
              <p className="text-sm text-muted-foreground">
                나가면 이 리스트와 리마인더가 더 이상 보이지 않습니다.
              </p>
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => setConfirmLeave(false)}>
                  취소
                </Button>
                <Button
                  variant="destructive"
                  disabled={removeMember.isPending}
                  onClick={() =>
                    removeMember.mutate(
                      { listId: list.id, userId: me.id },
                      { onSuccess: onLeft }
                    )
                  }
                >
                  나가기
                </Button>
              </div>
            </>
          ) : (
            <Button variant="outline" onClick={() => setConfirmLeave(true)}>
              <LogOutIcon />
              리스트 나가기
            </Button>
          )}
        </DialogFooter>
      )}
    </div>
  )
}

type InviteFormValues = {
  email: string
}

function InviteForm({ listId }: { listId: number }) {
  const invite = useInviteListMember()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InviteFormValues>({ defaultValues: { email: "" } })

  function onSubmit(values: InviteFormValues) {
    invite.mutate(
      { listId, email: values.email.trim() },
      { onSuccess: () => reset() }
    )
  }

  const errorMessage = errors.email?.message ?? inviteErrorMessage(invite.error)

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-1.5" noValidate>
      <Label htmlFor="invite-email">이메일로 초대</Label>
      <div className="flex gap-2">
        <Input
          id="invite-email"
          type="email"
          placeholder="name@example.com"
          autoComplete="off"
          aria-invalid={errorMessage ? true : undefined}
          {...register("email", {
            validate: (value) =>
              /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()) ||
              "이메일 형식으로 입력해 주세요.",
            onChange: () => invite.reset(),
          })}
        />
        <Button type="submit" disabled={invite.isPending}>
          <UserPlusIcon />
          초대
        </Button>
      </div>
      {errorMessage && <p className="text-xs text-destructive">{errorMessage}</p>}
      {invite.isSuccess && (
        <p className="text-xs text-muted-foreground">
          {invite.data.name}님을 초대했습니다.
        </p>
      )}
    </form>
  )
}

function inviteErrorMessage(error: Error | null): string | undefined {
  if (!error) return undefined
  if (error instanceof ApiError && error.status === 404) {
    return "해당 이메일로 가입한 사용자가 없습니다."
  }
  if (error instanceof ApiError && error.status === 400) {
    return "이미 이 리스트의 멤버입니다."
  }
  return "초대하지 못했습니다. 잠시 후 다시 시도해 주세요."
}
