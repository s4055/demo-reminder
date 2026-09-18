import { ReminderForm } from "@/components/reminder-form"
import { ReminderList } from "@/components/reminder-list"

export default function Home() {
  return (
    <div className="flex flex-1 items-start justify-center bg-zinc-50 px-4 py-16 dark:bg-black">
      <main className="flex w-full max-w-md flex-col gap-4">
        <h1 className="text-xl font-semibold">리마인더</h1>
        <ReminderForm />
        <ReminderList />
      </main>
    </div>
  )
}
