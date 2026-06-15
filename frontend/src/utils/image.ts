// src/utils/image.ts

/**
 * 图片压缩选项
 */
export interface CompressImageOptions {
  /** 最大宽度，默认 512 */
  maxWidth?: number
  /** 最大高度，默认 512 */
  maxHeight?: number
  /** 输出质量，默认 0.8 */
  quality?: number
  /** 输出 MIME 类型，默认 image/jpeg */
  outputType?: string
}

/**
 * 使用 Canvas 压缩图片文件。
 * 若压缩后文件比原文件更大，则返回原文件。
 * 压缩失败时回退到原文件，避免上传中断。
 */
export function compressImage(file: File, options: CompressImageOptions = {}): Promise<File> {
  const { maxWidth = 512, maxHeight = 512, quality = 0.8, outputType = 'image/jpeg' } = options

  return new Promise((resolve, reject) => {
    const img = new Image()
    const objectUrl = URL.createObjectURL(file)

    img.onload = () => {
      URL.revokeObjectURL(objectUrl)

      let { width, height } = img
      if (width > maxWidth || height > maxHeight) {
        const ratio = Math.min(maxWidth / width, maxHeight / height)
        width = Math.round(width * ratio)
        height = Math.round(height * ratio)
      }

      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height

      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('无法创建 canvas 上下文'))
        return
      }
      ctx.drawImage(img, 0, 0, width, height)

      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error('图片压缩失败'))
            return
          }
          const compressedFile = new File([blob], file.name, {
            type: outputType,
            lastModified: file.lastModified,
          })
          resolve(compressedFile.size < file.size ? compressedFile : file)
        },
        outputType,
        quality,
      )
    }

    img.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('图片加载失败'))
    }

    img.src = objectUrl
  })
}
