import type { AppData } from './store'
import type { Pet } from './data/models'
import saseumi from './assets/saseumi.jpg'
import saseumiFace from './assets/saseumi_face.jpg'
import saseumiWide from './assets/saseumi_wide.jpg'

/** 샘플 반려견(사슴이)의 실제 사진 — 보호자가 다른 사진을 올리면 그 사진을 씁니다 */
const DEFAULT_PHOTOS: Record<string, { face: string; wide: string; full: string }> = {
  'pet-1': { face: saseumiFace, wide: saseumiWide, full: saseumi },
}

export function petFace(pet?: Pet): string | null {
  if (!pet) return null
  return pet.photo ?? DEFAULT_PHOTOS[pet.id]?.face ?? null
}

/** 홈 여행 카드 배경 */
export function homeBackground(s: AppData): { src: string | null; position: string } {
  if (s.homeBg?.kind === 'custom' && s.homeBg.custom) return { src: s.homeBg.custom, position: '50% 50%' }
  const pet = s.pets[0]
  if (pet?.photo) return { src: pet.photo, position: '50% 35%' }
  const d = pet ? DEFAULT_PHOTOS[pet.id] : undefined
  return { src: d?.wide ?? null, position: '46% 38%' }
}

/** 사진 파일 → 정사각(프로필) 또는 가로(배경)로 잘라 용량을 줄인 dataURL */
export function readPhoto(file: File, mode: 'square' | 'wide'): Promise<string> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      const [tw, th] = mode === 'square' ? [480, 480] : [1200, 680]
      const scale = Math.max(tw / img.width, th / img.height)
      const w = tw / scale, h = th / scale
      const c = document.createElement('canvas')
      c.width = tw; c.height = th
      c.getContext('2d')!.drawImage(img, (img.width - w) / 2, (img.height - h) / 2, w, h, 0, 0, tw, th)
      URL.revokeObjectURL(url)
      resolve(c.toDataURL('image/jpeg', 0.82))
    }
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('사진을 열 수 없어요')) }
    img.src = url
  })
}
