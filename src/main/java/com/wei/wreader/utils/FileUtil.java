package com.wei.wreader.utils;

import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class FileUtil {
    /**
     * 将 BMP 数据转换为 JPG 并保存到本地
     * @param bmpData
     * @param savePath
     */
    public static String convertImgToJPG(byte[] bmpData, String savePath) {
        try {
            // 将 BMP 数据转换为 BufferedImage
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bmpData));
            // 将 BufferedImage 转换为 PNG 格式并编码为 Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            // 结尾后缀不是.jpg的，替换成.jpg
            if (!savePath.endsWith(".jpg") && !savePath.endsWith(".JPG") &&
                    !savePath.endsWith(".jpeg") && !savePath.endsWith(".JPEG")) {
                // 将 savePath 的后缀改为 .jpg
                savePath = savePath.substring(0, savePath.lastIndexOf(".")) + ".jpg";
            }
            // 将图片保存至本地
            FileUtils.writeByteArrayToFile(new File(savePath), baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return savePath;
    }
}
