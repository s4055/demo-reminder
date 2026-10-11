import {
  BabyIcon,
  BookOpenIcon,
  BookmarkIcon,
  BriefcaseIcon,
  CalendarIcon,
  CarIcon,
  CodeIcon,
  DumbbellIcon,
  FlagIcon,
  Gamepad2Icon,
  GiftIcon,
  GraduationCapIcon,
  HeartIcon,
  HouseIcon,
  LeafIcon,
  ListIcon,
  MusicIcon,
  PawPrintIcon,
  PillIcon,
  PlaneIcon,
  ShoppingCartIcon,
  StarIcon,
  UtensilsIcon,
  WalletIcon,
  type LucideIcon,
} from "lucide-react"

// 서버의 ListIcon.KEYS와 같은 목록이다. 이 밖의 값은 서버가 400으로 거부한다.
export const LIST_ICONS = {
  list: ListIcon,
  "shopping-cart": ShoppingCartIcon,
  briefcase: BriefcaseIcon,
  house: HouseIcon,
  heart: HeartIcon,
  star: StarIcon,
  "book-open": BookOpenIcon,
  gift: GiftIcon,
  plane: PlaneIcon,
  dumbbell: DumbbellIcon,
  music: MusicIcon,
  car: CarIcon,
  "graduation-cap": GraduationCapIcon,
  utensils: UtensilsIcon,
  baby: BabyIcon,
  pill: PillIcon,
  wallet: WalletIcon,
  calendar: CalendarIcon,
  flag: FlagIcon,
  bookmark: BookmarkIcon,
  "paw-print": PawPrintIcon,
  leaf: LeafIcon,
  code: CodeIcon,
  "gamepad-2": Gamepad2Icon,
} satisfies Record<string, LucideIcon>

export type ListIconKey = keyof typeof LIST_ICONS

export const LIST_ICON_KEYS = Object.keys(LIST_ICONS) as ListIconKey[]

// 모르는 값이 와도(이전 버전 데이터 등) 기본 표시로 떨어지도록 확인한다.
export function isListIconKey(icon: string | null): icon is ListIconKey {
  return icon !== null && Object.hasOwn(LIST_ICONS, icon)
}
