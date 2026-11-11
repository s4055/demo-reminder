import { describe, expect, it } from "vitest"
import {
  SMART_VIEWS,
  emptyMessage,
  isSameSelection,
  selectionKey,
  smartViewLabel,
  type Selection,
} from "@/lib/selection"

describe("smartViewLabel", () => {
  it("returnsKoreanLabel", () => {
    expect(smartViewLabel("today")).toBe("오늘")
    expect(smartViewLabel("flagged")).toBe("플래그 지정됨")
  })

  it("hasLabelForEverySmartView", () => {
    expect(SMART_VIEWS.map((item) => item.view)).toEqual([
      "today",
      "scheduled",
      "all",
      "flagged",
      "completed",
    ])
  })
})

describe("isSameSelection", () => {
  it.each<[Selection, Selection]>([
    [{ type: "list", listId: 1 }, { type: "list", listId: 1 }],
    [{ type: "smart", view: "today" }, { type: "smart", view: "today" }],
    [{ type: "tag", name: "업무" }, { type: "tag", name: "업무" }],
    [{ type: "search", query: "우유" }, { type: "search", query: "우유" }],
  ])("isTrueForSameValue %o", (a, b) => {
    expect(isSameSelection(a, b)).toBe(true)
  })

  it.each<[Selection, Selection]>([
    [{ type: "list", listId: 1 }, { type: "list", listId: 2 }],
    [{ type: "smart", view: "today" }, { type: "smart", view: "all" }],
    [{ type: "tag", name: "업무" }, { type: "search", query: "업무" }],
  ])("isFalseForDifferentValueOrType %o", (a, b) => {
    expect(isSameSelection(a, b)).toBe(false)
  })
})

describe("selectionKey", () => {
  it.each<[Selection, string]>([
    [{ type: "list", listId: 7 }, "list-7"],
    [{ type: "tag", name: "업무" }, "tag-업무"],
    [{ type: "search", query: "우유" }, "search-우유"],
    [{ type: "smart", view: "completed" }, "smart-completed"],
  ])("builds key for %o", (selection, expected) => {
    expect(selectionKey(selection)).toBe(expected)
  })
})

describe("emptyMessage", () => {
  it.each<[Selection, string]>([
    [{ type: "list", listId: 1 }, "이 리스트에 리마인더가 없습니다."],
    [{ type: "tag", name: "업무" }, "#업무 태그가 붙은 리마인더가 없습니다."],
    [{ type: "search", query: "우유" }, "“우유”와(과) 일치하는 리마인더가 없습니다."],
    [{ type: "smart", view: "today" }, "오늘 마감인 리마인더가 없습니다."],
    [{ type: "smart", view: "scheduled" }, "마감일이 설정된 리마인더가 없습니다."],
    [{ type: "smart", view: "flagged" }, "플래그 지정된 리마인더가 없습니다."],
    [{ type: "smart", view: "completed" }, "완료된 리마인더가 없습니다."],
    [{ type: "smart", view: "all" }, "리마인더가 없습니다."],
  ])("describes empty %o", (selection, expected) => {
    expect(emptyMessage(selection)).toBe(expected)
  })
})
