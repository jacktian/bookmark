package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;

import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import com.google.gson.JsonObject;

import java.net.HttpURLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URL {
	public String icon;
	public String title;
	
	public URL(String icon,String title){
		this.icon=icon;
		this.title=title;
	}
	/**
     * 获取icon和title
     * @param bUrl
     * @return
     */
    
    public static URL access(Long bookmarkId,String bUrl){
    	URL u=null;
    	String icon="",title="";
    	try {
			java.net.URL url=new java.net.URL(bUrl);
			HttpURLConnection c=(HttpURLConnection)url.openConnection();
			c.setRequestProperty("Connection", "Keep-Alive");
			c.setRequestProperty("Charset", "UTF-8");
			c.setRequestProperty("Content-Type", "text/html");
			c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36");
			c.setDoInput(true);
			c.setDoOutput(false);
			c.connect();
			System.out.println("连接指定url的状态码： "+c.getResponseCode());
			//获取doc成功，该书签有效
			if(c.getResponseCode()==200){
				//先判断根路径下的favicon.ico
				String rootIconUrl=url.getProtocol()+"://"+url.getHost()+"/favicon.ico";
				System.out.println(rootIconUrl);
				java.net.URL iUrl=new java.net.URL(rootIconUrl);
				HttpURLConnection ic=(HttpURLConnection)iUrl.openConnection();
				ic.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36");
				ic.connect();
				
				
				if(ic.getResponseCode()==200 && ic.getContentLength()>0){
					//获取根目录下的favicon.ico成功
					icon=rootIconUrl;
				}
					
				//再获取页面中的link元素
				Parser iparser=new Parser();
				iparser.setURL(bUrl);
				iparser.setEncoding("utf-8");
				//处理link元素
				TagNameFilter ifilter=new TagNameFilter("link");
				NodeList inodeList=(NodeList) iparser.parse(ifilter);
				for(int i=0;i<inodeList.size();i++){
					String link=inodeList.elementAt(i).getText();
					System.out.println(link);
					if(link.indexOf("rel=\"shortcut icon\"")!=-1 || link.indexOf("rel=\"icon\"")!=-1){
						Pattern p=Pattern.compile("href=\"([\\S]+)\"");
						Matcher m=p.matcher(link);
						while(m.find()){
							String iconHref=m.group(1);
							System.out.println("href= "+iconHref);
							//检查是否以绝对路径开始
							if(iconHref.startsWith("http://") || iconHref.startsWith("https://") || iconHref.startsWith("ftp://") || iconHref.startsWith("//")){
								icon=iconHref;
							}else{
								//增加对路径的判断，如：对于xx.com/latest/ 而言 /favicon.ico与favicon.ico的起始位置不相同
								if(iconHref.startsWith("/")){
									icon=url.getProtocol()+"://"+url.getHost()+iconHref;
								}else{
									icon=bUrl+iconHref;
								}
							}
						}
					}
				}
				
				//解析html
				Parser parser=new Parser();
				parser.setURL(bUrl);
				
				parser.setEncoding("utf-8");
				//获取页面的title元素
				TagNameFilter filter=new TagNameFilter("title");
				NodeList nodeList=parser.parse(filter);
				for(int i=0;i<nodeList.size();i++){
					title=nodeList.elementAt(i).toPlainTextString();
					System.out.println("获取到的title内容："+title);
				}
				
				ic.disconnect();
				c.disconnect();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		u=new URL(icon, title);
		if(u!=null && bookmarkId!=null){
			//修改现有书签的title和icon信息
			Bookmark bookmark=Bookmark.findById(bookmarkId);
			bookmark.icon=icon;
			bookmark.title=title;
			bookmark.save();
		}
    	return u;
    }
}