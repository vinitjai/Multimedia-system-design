
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class imageReader {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage img;
	BufferedImage outputImage;

	 private static final int YSample = 0;
	 private static final int USample = 1;
	 private static final int VSample = 2;	
	 private static final int MaxV = 255;	

	class YUV_Pixel {
		float y, u, v;
		public YUV_Pixel(float y,float u, float v) {
			this.y = y;
			this.u = u;
			this.v = v;
		}
	}
	
	class RGB_Pixel {
		int r, g, b;
		public RGB_Pixel(int r, int g, int b){
			this.r = r;
			this.g= g;
			this.b = b;
		}
	}

	public void showIms(String[] args){
		int Yinput = Integer.parseInt(args[1]);
		int Uinput = Integer.parseInt(args[2]);
		int Vinput = Integer.parseInt(args[3]);
		int Q = Integer.parseInt(args[4]);

		int default_width = 352;  
		int default_height = 288; 	

		img = new BufferedImage(default_width, default_height, BufferedImage.TYPE_INT_RGB);

		try {
			File file = new File(args[0]);	//input image
			InputStream is = new FileInputStream(file);

			long len = file.length();
			byte[] bytes = new byte[(int)len];


			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length - offset)) >= 0) {	
				offset += numRead;
			}

			int ind = 0;
			RGB_Pixel[][] givenRGB = new RGB_Pixel[default_height][default_width];
			YUV_Pixel[][] resultYUV = new YUV_Pixel[default_height][default_width];

			for(int y = 0; y < default_height; y++) {
				for(int x = 0; x < default_width; x++) {

					int R = bytes[ind];			//R
					int G = bytes[ind + default_height*default_width];	//G
					int B = bytes[ind + default_height*default_width*2];  //B
					
					int pixel = 0xff000000 | ((R & 0xff) << 16) | ((G & 0xff) << 8) | (B & 0xff);					
					img.setRGB(x,y,pixel);

					R = getUnsignedint(R);
					G = getUnsignedint(G);
					B = getUnsignedint(B);

					RGB_Pixel rgbObj = new RGB_Pixel(R, G, B);
					givenRGB[y][x] = rgbObj;
					
					float[] YUVArray = RBGtoYUVConversion(R, G, B);
					YUV_Pixel YUV = new YUV_Pixel(YUVArray[0], YUVArray[1], YUVArray[2]);
					resultYUV[y][x] = YUV;

					ind++;
				}
			}

		
			subSample(default_height, default_width, resultYUV, Yinput, Uinput, Vinput);
			
			outputImage = new BufferedImage(default_width, default_height, BufferedImage.TYPE_INT_RGB);
			
			for(int i = 0; i < default_height; i++) {
				for(int j = 0; j < default_width; j++) {

					YUV_Pixel yuv = resultYUV[i][j];
					
					int[] RGB_Array = YUVtoRGBConversion(yuv.y, yuv.u, yuv.v);
					int R = RGB_Array[0];
					int G = RGB_Array[1];
					int B = RGB_Array[2];	

					int[] quantizedRGB = quantize(R, G, B, Q);

					R = quantizedRGB[0];
					if(R < 0) R = 0;
					else if(R > 255) R = 255;

					G = quantizedRGB[1];
					if(G < 0) G = 0;
					else if(G > 255) G = 255;

					B = quantizedRGB[2];
					if(B < 0) B = 0;
					else if(B > 255) B = 255;
					
				int Pix = 0xff000000 | ((R & 0xff) << 16) | ((G & 0xff) << 8) | (B & 0xff);					
					outputImage.setRGB(j, i, Pix);
					
				}
			}
			
			is.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}



		// Use labels to display the images
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Original image (Left)");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Image after modification (Right)");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		lbIm1 = new JLabel(new ImageIcon(img));
		lbIm2 = new JLabel(new ImageIcon(outputImage));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);

	}

	public static int getUnsignedint(int x){
		return x & 0xFF;
	}

	private YUV_Pixel[][] upSampleTesting(YUV_Pixel[][] resultYUV, int space, int default_width, int i, int j, int sample) {
		int rem = j % space;
		if(rem != 0) {
			int prev = j-1;
			int next = j+1; 

			if(next < default_width && prev >= 0) {
				YUV_Pixel prevYUV = resultYUV[i][prev];
				YUV_Pixel currentYUV = resultYUV[i][j];
				YUV_Pixel nextYUV = resultYUV[i][next];
			
				switch(sample) {
					case YSample: {
						currentYUV.y = (prevYUV.y + nextYUV.y)/2;
							break;
							}
					case USample: {
						currentYUV.u = (prevYUV.u + nextYUV.u)/2;
							break;
							}
					case VSample: {
						currentYUV.y = (prevYUV.v + nextYUV.v)/2;
							break;
							}
					default : //should never happen

				}	

			} 
		}
		return resultYUV;
	}



	private YUV_Pixel[][] upSampleTestingRetainPrevPixels(YUV_Pixel[][] resultYUV, int space, int default_width, int i, int j, int sample) {
		int k = j % space;
		if(k != 0) {
			int prev = j-1;
			int next = j+1; 

			if(next < default_width && prev >= 0) {
				YUV_Pixel prevYUV = resultYUV[i][prev];
				YUV_Pixel currentYUV = resultYUV[i][j];
			
				switch(sample) {
					case YSample: {
						currentYUV.y = prevYUV.y ;
							break;
							}
					case USample: {
						currentYUV.u = prevYUV.u;
							break;
							}
					case VSample: {
						currentYUV.y = prevYUV.v;
							break;
							}
					default : //should never happen

				}	

			} 
		}
		return resultYUV;
	}

private int[] quantize(int inputR, int inputG, int inputB, int Q) {
      if(Q < 256 && Q > 0) {
            int partition = (256/Q);
            if(inputR > (MaxV-partition)) {
                inputR = MaxV-partition;
            }else{
            	int level = (int)Math.rint((inputR+1.0)/partition);
            	inputR = partition*level-1;
	    	if(inputR > MaxV){
                        inputR = MaxV;
            	}else if(inputR < 0){
                        inputR = 0;
            	}
	   }
            if(inputG > (MaxV-partition)) {
                inputG = MaxV-partition;
            }else{
            	int level = (int)Math.rint((inputG+1.0)/partition);
            	inputG = partition*level-1;
	    	if(inputG > MaxV){
                        inputG = MaxV;
            	}else if(inputG < 0){
                        inputG = 0;
            	}
	   }
            if(inputB > (MaxV-partition)) {
                inputB = MaxV-partition;
            }else{
            	int level = (int)Math.rint((inputB+1.0)/partition);
            	inputB = partition*level-1;
	    	if(inputB > MaxV){
                        inputB = MaxV;
            	}else if(inputB < 0){
                        inputB = 0;
            	}
	    }

      }
 	int[] Toreturn = new int[]{inputR,inputG,inputB};
        return Toreturn;

} 

	private float[] RBGtoYUVConversion(int R, int G, int B) {
		float[] YUV = new float[3];
		YUV[0] = (float) (0.299*R+0.587*G+0.114*B);
		YUV[1] = (float) (0.596*R+(-0.274*G)+(-0.322*B));
		YUV[2] = (float) (0.211*R+(-0.523*G)+0.312*B);
		return YUV;
	}

	private int[] YUVtoRGBConversion(float Y, float U, float V) {
		int[] RGB = new int[3];
		RGB[0] = (int) (1.000*Y+0.956*U+0.621*V);
		RGB[1] = (int) (1.000*Y+(-0.272*U)+(-0.647*V));
		RGB[2] = (int) (1.000*Y+(-1.106*U)+(1.703*V));
		return RGB;
	}

private void upSample(int default_width,YUV_Pixel[][] resultYUV,int Yinput,int Uinput,int Vinput,int i,int j) {
				resultYUV = upSampleTesting(resultYUV, Yinput, default_width, i, j, YSample);
				resultYUV = upSampleTesting(resultYUV, Uinput, default_width, i, j, USample);
				resultYUV = upSampleTesting(resultYUV, Vinput, default_width, i, j, VSample);
}

	private void subSample(int default_height, int default_width,YUV_Pixel[][] resultYUV,int Yinput,int Uinput,int Vinput) {
			for(int i = 0; i < default_height; i++) {
				for(int j = 0; j < default_width; j++) {
					upSample(default_width,resultYUV,Yinput,Uinput,Vinput,i,j);
				}
			}
	}


	public static void main(String[] args) {
		imageReader ren = new imageReader();
		ren.showIms(args);
	}
	

}
