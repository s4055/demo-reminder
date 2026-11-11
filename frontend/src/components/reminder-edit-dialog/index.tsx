"use client"

import { Dialog, DialogContent } from "@/components/ui/dialog"
import { ReminderEditForm } from "@/components/reminder-edit-dialog/reminder-edit-form"
import type { Reminder } from "@/lib/reminders-api"

export function ReminderEditDialog({
  open,
  onOpenChange,
  reminder,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  reminder: Reminder
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <ReminderEditForm
          key={reminder.id}
          reminder={reminder}
          onSaved={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  )
}
