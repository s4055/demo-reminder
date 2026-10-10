import { useEffect, useState } from "react"

// value가 delayMs 동안 바뀌지 않으면 그 값을 돌려준다. 입력할 때마다 요청하지 않도록 검색어에 쓴다.
export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])
  return debounced
}
