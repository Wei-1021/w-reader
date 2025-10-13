package com.wei.wreader.utils.file;

import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtil {
    private static final Logger LOGGER = Logger.getLogger(FileUtil.class.getName());
    private static final String JPG_EXTENSION = ".jpg";
    private static final String JPEG_EXTENSION = ".jpeg";

    /**
     * 将图片数据转换为 JPG 并保存到本地
     * @param bmpData 图片的字节数组数据
     * @param savePath 图片保存的路径
     * @return 最终保存图片的路径
     */
    public static String convertImgToJPG(byte[] bmpData, String savePath) {
        if (bmpData == null || bmpData.length == 0) {
            LOGGER.warning("输入的图片数据为空，无法进行转换。");
            return null;
        }
        if (savePath == null || savePath.isEmpty()) {
            LOGGER.warning("保存路径为空，无法进行保存。");
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bmpData);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 将 BMP 数据转换为 BufferedImage
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                LOGGER.warning("无法从输入的字节数据中读取图片。");
                return null;
            }

            // 将 BufferedImage 转换为 JPG 格式
            boolean isWritten = ImageIO.write(image, "jpg", baos);
            if (!isWritten) {
                LOGGER.warning("无法将图片转换为 JPG 格式。");
                return null;
            }

            // 确保保存路径的后缀为 .jpg
            savePath = ensureJpgExtension(savePath);

            // 将图片保存至本地
            FileUtils.writeByteArrayToFile(new File(savePath), baos.toByteArray());
            LOGGER.info("图片已成功保存至: " + savePath);
            return savePath;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "保存图片时发生 I/O 异常: " + savePath, e);
            return null;
        }
    }

    /**
     * 确保保存路径的后缀为 .jpg
     * @param path 输入的保存路径
     * @return 后缀为 .jpg 的保存路径
     */
    private static String ensureJpgExtension(String path) {
        String lowerCasePath = path.toLowerCase();
        if (!lowerCasePath.endsWith(JPG_EXTENSION) && !lowerCasePath.endsWith(JPEG_EXTENSION)) {
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex != -1) {
                path = path.substring(0, lastDotIndex) + JPG_EXTENSION;
            } else {
                path = path + JPG_EXTENSION;
            }
        }
        return path;
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 判断是否为不支持的图片格式
     *
     * @param key 文件名
     * @return 是否为不支持的图片格式
     */
    public static boolean isUnsupportedImageFormat(String key) {
        String lowerKey = key.toLowerCase();
        return switch (getFileExtension(lowerKey)) {
            case "bmp", "webp", "ico", "tiff", "avif" -> true;
            default -> false;
        };
    }

}
