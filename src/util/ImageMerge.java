package util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;

import javax.imageio.ImageIO;

import common.ErrorHandling;

/**
 * 다운받은 만화를 하나의 세로로 긴 파일로 통합시킨 이미지를 생성하는 클래스
 * @author occidere
 *
 */
public class ImageMerge{
	private File root = null; //병합할 이미지들이 들어있는 폴더
	private String imgExt[] = {"jpg", "jpeg", "bmp", "png", "gif"}; //지원되는 확장자
	
	public ImageMerge() {
		this(System.getProperty("user.dir"));
	}
	
	public ImageMerge(String rootPath) {
		root = new File(rootPath);
	}
	
	@SuppressWarnings("unused")
	public void mergeAll(String mergedName) {
		try {
			System.out.print("이미지 병합중 ... ");
			
			int i, len, preHeight, width, height, maxWidth = -1, totalHeight = -1;
			
			/* 폴더 내부의 모든 이미지(jpg, bmp등)를 얻음 */
			File images[] = root.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					for(String ext : imgExt)
						if(name.toLowerCase().endsWith(ext)) return true;
					return false;
				}
			});
			
			BufferedImage bi[] = new BufferedImage[len = images.length];
			for(i=0;i<len;i++) {
				bi[i] = ImageIO.read(images[i]);
				maxWidth = Math.max(maxWidth, width = bi[i].getWidth()); //생성될 통합파일의 너비 계산
				totalHeight+=(height = bi[i].getHeight()); //생성될 통합파일의 총 높이 계산
			}
			
			/* 통합 이미지. 최대 너비 x 총 높이 */
			BufferedImage mergedImage = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = (Graphics2D) mergedImage.getGraphics();
			graphics.setBackground(Color.BLACK); //여백은 검정으로 채움
			
			graphics.drawImage(bi[0], 0, 0, null); //처음엔 (0, 0)에 그리기
			preHeight = bi[0].getHeight(); 
			
			for(i=1;i<len;i++) {
				graphics.drawImage(bi[i], 0, preHeight, null);
				preHeight += bi[i].getHeight(); //높이를 누산해야됨
			}
			
			/* 병합된 이미지를 최종적으로 write */
			ImageIO.write(mergedImage, "jpg", new File(root+"/" + mergedName + ".jpg"));
			
		} catch (Exception e) {
			ErrorHandling.saveErrLog("이미지 병합 실패: "+mergedName, "", e);
			return;
		}
		System.out.println("성공");
	}
}