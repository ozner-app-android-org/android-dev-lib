package com.ozner.device;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.ozner.util.dbg;

public class FirmwareTools {
	public class FirmwareExcpetion extends Exception
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FirmwareExcpetion(String message)
		{
			super(message);
		}
	}
	public String Platform;
	public long Firmware;
	byte[] bytes;
	public FirmwareTools(String path) throws FirmwareExcpetion, IOException
	{
		File file=new File(path);
		byte[] key={0x23,0x23,0x24,0x24,0x40,0x40,0x2a,0x2a,0x43,0x75,0x70,0x00};
		if (file.length()>127*1024) throw new FirmwareExcpetion("文件太大");
		
		FileInputStream fs=new FileInputStream(path);
		try
		{
			bytes=new byte[(int)file.length()];
			fs.read(bytes);
			for (int i=0;i<bytes.length-key.length;i++)
			{
				boolean found=false;
				for (int x=0;x<key.length;x++)
				{
					if (key[x]==bytes[i+x])
					{
						found=true;
						
					}else
					{
						found=false;
						break;
					}
				}
				
				if (!found)
				{
					throw new FirmwareExcpetion("错误的文件");
				}
				else
				{
					String temp=new String(bytes,i+16,18,Charset.forName("US-ASCII"));
					if (temp=="") return;
					try
					{
						Platform=temp.substring(0, 2);
						String mon=temp.substring(3,6);
						String day=temp.substring(6,8);
						String year=temp.substring(8,12);
						String hour=temp.substring(12,14);
						String min=temp.substring(14,16);
						String sec=temp.substring(16,18);
						
						SimpleDateFormat df=new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss",Locale.US);
						Date date= df.parse(year+"-"+mon+"-"+day+" "+hour+":"+min+":"+sec);
						Firmware=date.getTime();
					}catch (Exception e) {
						dbg.e(e.toString());
					}
					
				}
			}
		}finally
		{
			fs.close();
		}
	}
}
