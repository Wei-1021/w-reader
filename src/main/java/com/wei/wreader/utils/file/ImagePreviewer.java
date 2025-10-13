package com.wei.wreader.utils.file;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.ui.MessageDialogUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * 图片预览器
 *
 * @author weizhanjie
 * @since 2025/03/20
 */
public class ImagePreviewer {
    private Project project;
    private JLabel imageLabel;
    private Image image;
    private double scale = 1.0;
    private int lastX, lastY;

    public ImagePreviewer(Project project) {
        this.project = project;
    }

    public ImagePreviewer(Project project, String imagePath) {
        this.project = project;
        try {
            // 判断是否是URL, 如果是则使用URL读取图片
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file://")) {
                image = ImageIO.read(new URI(imagePath).toURL());
            } else {
                image = ImageIO.read(new File(imagePath));
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public ImagePreviewer(Project project, File file) {
        this.project = project;
        try {
            // 读取图片
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ImagePreviewer(Project project, URL url) {
        this.project = project;
        try {
            // 读取图片
            image = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ImagePreviewer(Project project, BufferedImage image) {
        this.project = project;
        this.image = image;
    }

    public void setUrlPath(String url) {
        try {
            image = ImageIO.read(new URI(url).toURL());
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void openImagePreview() {
        // 原始宽度
        int width = image.getWidth(null);
        // 缩放宽度
        int scaleWidth = ConstUtil.IMAGE_PREVIEW_WIDTH - 50;
        // 原始高度
        int height = image.getHeight(null);
        // 缩放高度
        int scaleHeight = ConstUtil.IMAGE_PREVIEW_HEIGHT - 80;
        // 计算缩放比例（宽度缩放比例和高度缩放比例中选择缩放比例高的）
        double initScale = Math.min((double) scaleWidth / width, (double) scaleHeight / height);
        image = ImageUtil.scaleImage(image, initScale);
        // 重置图片尺寸，以适应窗口大小
//        image = image.getScaledInstance(scaleWidth, scaleHeight, Image.SCALE_SMOOTH);
        // 创建 JLabel 用于显示图片
        imageLabel = new JLabel(new ImageIcon(image));

        // 创建 JScrollPane 用于滚动显示图片
        JBScrollPane scrollPane = new JBScrollPane(imageLabel);

        // 添加鼠标滚轮监听器
        scrollPane.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            if (notches < 0) {
                // 鼠标滚轮向上滚动，放大图片
                scale *= 1.1;
            } else {
                // 鼠标滚轮向下滚动，缩小图片
                scale /= 1.1;
            }
            // 确保缩放比例不小于 0.1
            scale = Math.max(0.1, scale);
            updateImage();
        });


        // 获取 JViewport
        JViewport viewport = scrollPane.getViewport();

        // 添加鼠标监听器以实现拖动功能
        viewport.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 记录鼠标按下时的坐标
                lastX = e.getX();
                lastY = e.getY();
            }

        });

        viewport.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // 获取当前滚动面板的滚动条
                JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
                JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();

                // 计算鼠标拖动的偏移量
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;

                // 根据偏移量调整滚动条的位置
                horizontalScrollBar.setValue(horizontalScrollBar.getValue() - dx);
                verticalScrollBar.setValue(verticalScrollBar.getValue() - dy);

                // 更新上次鼠标位置
                lastX = e.getX();
                lastY = e.getY();
            }
        });

        scrollPane.setBorder(JBUI.Borders.empty());
        MessageDialogUtil.showMessage(project, "图片预览(鼠标滚轮放大/缩小)", scrollPane,
                ConstUtil.IMAGE_PREVIEW_WIDTH, ConstUtil.IMAGE_PREVIEW_HEIGHT, true);
    }

    private void updateImage() {
        // 根据缩放比例调整图片大小
        int newWidth = (int) (image.getWidth(null) * scale);
        int newHeight = (int) (image.getHeight(null) * scale);
        Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
    }

}