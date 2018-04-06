
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class DCTCoderDecoder {


	JFrame frame;
	JLabel label1;
	JLabel label2;

	int ImageWidth=352;
	int ImageHeight=288;

	String InputImage;
	int QuantizationLevel;
	int QunatizationTableValue;
	int DeliveryMode;
	long Latency;	

	BufferedImage OutputImage = new BufferedImage(ImageWidth, ImageHeight, BufferedImage.TYPE_INT_RGB);
	BufferedImage im = new BufferedImage(ImageWidth, ImageHeight, BufferedImage.TYPE_INT_RGB);

	int[][][] InputImageMatrix=new int[3][ImageHeight][ImageWidth];
	int[][][] DCT_Matrix=new int[3][ImageHeight][ImageWidth];
	int[][][] OutputImageMatrix=new int[3][ImageHeight][ImageWidth];

	int BlockSize=64;
	int NumberOfHorizontalBlocks=ImageWidth/8;
	int NumberOfVertcalBlocks=ImageHeight/8;

	DCTCoderDecoder(String[] args)
	{

		InputImage=args[0];
		QuantizationLevel = Integer.parseInt(args[1]);
		QunatizationTableValue = 1<<QuantizationLevel;  //2 power N
		DeliveryMode=Integer.parseInt(args[2]);
		Latency=Integer.parseInt(args[3]);	
	
		readInputImage();
		InitialiseCosineMatrix();
		performDicreeteCosineTransform();
		DecoderDisplay();
	}


	public  void readInputImage()
        {
	    try {
		    File file = new File(InputImage);
		    InputStream is = new FileInputStream(file);
		    long len = file.length();
		    byte[] bytes = new byte[(int)len];
		    int offset = 0;
	            int numRead = 0;
	            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	              offset += numRead;
	            }	
	    	    int ind = 0;
			for(int y = 0; y < ImageHeight; y++){
				for(int x = 0; x < ImageWidth; x++){
					byte r = bytes[ind];
					byte g = bytes[ind+ImageHeight*ImageWidth];
					byte b = bytes[ind+ImageHeight*ImageWidth*2]; 

					InputImageMatrix[0][y][x]=r&0xff;
					InputImageMatrix[1][y][x]= g&0xff;
					InputImageMatrix[2][y][x]=b&0xff; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					im.setRGB(x,y,pix);
					ind++;
				}
			}
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	}

	public  void InitialiseCosineMatrix()
        {
		for(int x=0;x<8;x++){  
		    for(int u=0;u<8;u++){  
		        for(int y=0;y<8;y++){  
		            for(int v=0;v<8;v++){  
				double first = Math.PI*(2*x+1)*u*0.0625;
				double second = Math.PI*(2*y+1)*v*0.0625;
		                Cosine_Matrix[u][v][x][y]=(Math.cos(first)*Math.cos(second));
		            }
		        }
		    }
		}
	}

	public  void performDicreeteCosineTransform()
        {
	    for(int channel=0;channel<3;channel++){
			DCTPerChannel(channel);
	     }
	}

	void DCTPerChannel(int channel) {
	    	for(int y=0;y<NumberOfVertcalBlocks;y++){
	    	   for(int x=0;x<NumberOfHorizontalBlocks;x++){
				DCTPerBlock(channel,y,x);
		   }
		}
	}

	void DCTPerBlock(int channel,int y,int x) {
	    	for(int v=0;v<8;v++){
	    		for(int u=0;u<8;u++){
	    			double summation=0, c=0;
	    			if(u!=0 && v!=0) c=0.25;
	    			else if((u==0&&v!=0)||(u!=0&&v==0)) c=0.25*0.707;
	    			else c=0.125;

	    			for(int j=0;j<8;j++){
	    			  for(int i=0;i<8;i++)
	    				summation= summation +
                                                   InputImageMatrix[channel][j+y*8][i+x*8] * Cosine_Matrix[v][u][j][i];
	    			 }

	    			DCT_Matrix[channel][v+y*8][u+x*8] = (int) (summation*c/QunatizationTableValue);
	    		     }
	      }
	 }
	    	 

	public  void BlockByBlockIDCT(int y,int x)
        {
	    for(int channel=0;channel<3;channel++){
	    	for(int j=0;j<8;j++){		
	    		for(int i=0;i<8;i++){
	    			double summation=0, c=0;
	    			int v=0,u=0,bz=0;
	    			boolean traverse = true;
	    			while(u<8 || v<8){
	    	    			if(u!=0 && v!=0) c=0.25;
	    	    			else if((u==0&&v!=0)||(u!=0&&v==0)) c=0.25*0.707;
	    	    			else c=0.125;
	    	    					
	    	    			summation = summation + DCT_Matrix[channel][v+y][x+u]*Cosine_Matrix[v][u][j][i]*c;

	    				if(bz++==63) break;
	    							
					if(traverse){
	    					if(u==8-1){
	    						v++;
	    						traverse=false;
	    						continue;
	    					 }
	    					if(v==0){
	    						u++;
	    						traverse=false;
	    						continue;
	    					}
	    					v--;
	    					u++;
	    					continue;
	    				} else{
	    					if(v==8-1){
	    						u++;
	    						traverse=true;
	    						continue;
	    					}
	    					if(u==0){
	    						v++;
	    						traverse=true;
	    						continue;
	    					}
	    					u--;
	    					v++;
	    					continue;
	    				}
	    			}
	    	OutputImageMatrix[channel][j+y][i+x] =(int) summation*QunatizationTableValue;
	    	OutputImage.setRGB(x+i, y+j, OutputImage.getRGB(x+i, y+j)|((OutputImageMatrix[channel][j+y][i+x]<<(8*(2-channel)))));
	    		  }
	    		}
	    	}    		
	}


	public  void SuccessiveBitIDCT(int y,int x,int MaxBit)
        {
		int BitsTodisplay=0;
		for(int i=32,count=0;i>=1;i--){
			BitsTodisplay+=(1<<i);
			count++;
		       if(count==MaxBit)
			break;
	        }
	    for(int channel=0;channel<3;channel++){
	    	int rgbbits=0;
	    	if(channel==0) rgbbits=0xff00ffff;
	    	else if(channel==1) rgbbits=0xffff00ff;	
	    	else rgbbits=0xffffff00;
	    	
	    	  for(int j=0;j<8;j++){		
	    		for(int i=0;i<8;i++){
	    			 double summation=0, c=0;
	    			 int v=0,u=0,count1 = 0;
	    			 boolean traverse = true;
	    		         while(u<8 || v<8){
	    	    			if(u!=0 && v!=0) c=0.25;
	    	    			else if((u==0&&v!=0)||(u!=0&&v==0)) c=0.25*0.707;
	    	    			else c=0.125;
	    	    					
	    	    		summation=summation+(DCT_Matrix[channel][v+y][x+u]&BitsTodisplay)*Cosine_Matrix[v][u][j][i]*c;
	    			if(count1++==63) break;
	    			if(traverse){
	    				if(u==7){
	    					v++;
	    		                        traverse = false;
	    				}
	    				else if(v==0){
	    					u++;
	    					traverse = false;
	    				}
	    				else { v--;
	    				       u++;
                                        }
	    			}
	    			else{
	    				if(v==8-1){
	    					u++;
	    					traverse = true;
	    				 }
	    				else if(u==0){
	    					v++;
	    					traverse=true;
	    				}
	    				else { u--;
	    			 	       v++;
                                        }
	    			}

			       continue;
	    		}
	    			OutputImageMatrix[channel][j+y][i+x]=(int)(summation*QunatizationTableValue);
	    			int rgb=OutputImage.getRGB(x+i, y+j)&rgbbits;
	    			OutputImage.setRGB(x+i, y+j, rgb|((OutputImageMatrix[channel][j+y][i+x]<<(8*(2-channel)))));
	    				}
	    			}
	    		}    		
	}
	

	public  void SpectralIDCT(int y,int x,int limit)
        {
	    for(int channel=0;channel<3;channel++){
	    	int rgbbits=0;
	    	if(channel==0) rgbbits=0xff00ffff;
	    	else if(channel==1) rgbbits=0xffff00ff;
	    	else rgbbits=0xffffff00;
	    	
	    	for(int j=0;j<8;j++){		
	    		for(int i=0;i<8;i++){
	    			double summation=0, c=0;
	    			int v=0, u=0,bs=0;
	    			boolean traverse=true;
	    			while(u<8 || v<8){
	    	    			if(u!=0 && v!=0) c=0.25;
	    	    			else if((u==0&&v!=0)||(u!=0&&v==0)) c=0.25*0.707;
	    	    			else c=0.125;
	    	    					
	    	    			summation = summation + DCT_Matrix[channel][v+y][x+u]*Cosine_Matrix[v][u][j][i]*c;
	    				if(bs++==limit) break;
	    				if(traverse){
	    					if(u==8-1){
	    						v++;
	    						traverse=false;
	    					}
	    					else if(v==0){
	    						u++;
	    						traverse=false;
	    					}else{
	    						v--;
	    						u++;
						}
	    				 } else{
	    					if(v==8-1){
	    					       u++;
	    					       traverse=true;
	    					}else if(u==0){
	    						v++;
	    						traverse=true;
	    					}else{
	    						u--;
	    						v++;
						 }
	    					}
					     continue;
	    				  }
	    				OutputImageMatrix[channel][j+y][i+x] = (int) summation*QunatizationTableValue;
	    				int rgb=OutputImage.getRGB(x+i, y+j)&rgbbits;
	    				OutputImage.setRGB(x+i, y+j, rgb|((OutputImageMatrix[channel][j+y][i+x]<<(8*(2-channel)))));
	    				}
	    			}
	    		}    		
	}
	
	public void ImageFrames() {

	    frame = new JFrame();
	    label1 = new JLabel(new ImageIcon(im));
	    frame.getContentPane().add(label1, BorderLayout.WEST);
	    label2 = new JLabel(new ImageIcon(OutputImage));
	    frame.getContentPane().add(label2, BorderLayout.EAST);

	    frame.pack();
	    frame.setVisible(true);

	}

	public void HandleSequentialMode() {

	    ImageFrames();
	    /* Decoding is done block by block. 
               So we will iterate through one block at a time and decode DC and all ACs in that block and update UI 
             */

	    for(int j=0;j<NumberOfVertcalBlocks;j++){
	    	  for(int i=0;i<NumberOfHorizontalBlocks;i++){
	    		BlockByBlockIDCT(j*8,i*8);
	    		label2 = new JLabel(new ImageIcon(OutputImage));
	    		frame.getContentPane().add(label2, BorderLayout.EAST);
	    		label2.updateUI();
	    	    		try {
	    				Thread.sleep(Latency);
	    			 } catch (Exception e) {
	    			}
	    	   }
		}
        }


	public void HandleProgressiveSpectralSelectionMode() {

	    ImageFrames();

	    /* Total Number of Coefficients in each block will be equal to BlockSize (8*8 = 64).
	       We will iterate through all blocks for 64 times each time decoding one coefficient.
	       first we will decode DC coeffecients of all blocks then display and sleep for latency milliseconds.
	       Then Move to first AC coefficient, then next coeffecient till we diplsay all coeffecients(64).   
	    */

	    for(int coefficient=0;coefficient<BlockSize;coefficient++){
			
	    	   for(int j=0;j<NumberOfVertcalBlocks;j++){
	    		for(int i=0;i<NumberOfHorizontalBlocks;i++){
	    				SpectralIDCT(j*8,i*8,coefficient);
	    			}
	    		}
			label2 = new JLabel(new ImageIcon(OutputImage));
			frame.getContentPane().add(label2, BorderLayout.EAST);
			label2.updateUI();
			try {
				Thread.sleep(Latency);
			    } catch (Exception e) {
			   }
	    	}
	}

	public void HandleProgressiveSuccessiveBitApproximationMode() {
	    ImageFrames();

	    /*  All DC and AC coefficients of all image blocks are decoded first and displayed in a successive-bit manner.
		We will iterate through all blocks but only use one bit and in next we will use first and second bit.
		Update the UI and sleep for latency milliseconds.
            */

    	    for(int MaxBit=0;MaxBit<32;MaxBit++){
    			for(int j=0;j<NumberOfVertcalBlocks;j++){
    				for(int i=0;i<NumberOfHorizontalBlocks;i++){
    						SuccessiveBitIDCT(j*8,i*8,MaxBit);
    				}
    			}

			label2 = new JLabel(new ImageIcon(OutputImage));
			frame.getContentPane().add(label2, BorderLayout.EAST);
			label2.updateUI();
			try{
				Thread.sleep(Latency);
			}catch(Exception e){
			}
    		}
	 }

	public  void DecoderDisplay()
        {
	    switch(DeliveryMode) { 
	     case 1:{
			HandleSequentialMode(); break;
		    }
	    case 2:{
			HandleProgressiveSpectralSelectionMode(); break;
		   }
	    case 3:{
			HandleProgressiveSuccessiveBitApproximationMode(); break;
		   }
	    default: //should never happen 

	     }
	}

	public static void main(String[] args) 
        {
		DCTCoderDecoder dctcd = new DCTCoderDecoder(args);
	}


	double[][][][] Cosine_Matrix = new double[8][8][8][8];


}

