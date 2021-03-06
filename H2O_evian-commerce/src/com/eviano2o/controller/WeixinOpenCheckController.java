package com.eviano2o.controller;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eviano2o.controller.helper.IndexComponentHelper;
import com.eviano2o.util.HttpClientUtil;
import com.eviano2o.util.SysParamNames;
import com.eviano2o.util.WxTokenAndJsticketCache;
import com.eviano2o.wxDeciphering.AesException;
import com.eviano2o.wxDeciphering.WXBizMsgCrypt;

/** 
 * <p>ClassName: 测试微信 全网检测 </p>  
 * <p>Description: 检测通过后 才能进行全网发布 </p> 
 */  
@Controller  
@RequestMapping(value = "/weixinOpenCheck")  
public class WeixinOpenCheckController extends BaseController {  
      
    private static final Logger logger = LoggerFactory.getLogger(WeixinOpenCheckController.class);  
       
      
      
    @ResponseBody  
    @RequestMapping(value = "verify_new/{appid}/callback", method = RequestMethod.POST)  
    public void acceptMessageAndEvent(Model model, HttpServletRequest request, HttpServletResponse response  
            ,@PathVariable(value = "appid") String appId)//springMVC 获取地址里面的参数信息  
                    throws IOException, AesException, DocumentException {  
          
        String nonce = request.getParameter("nonce");  
        String timestamp = request.getParameter("timestamp");  
        String msgSignature = request.getParameter("msg_signature");  
              
        //logger.info("读取数据为："+"msg_signature="+msgSignature+",  timestamp="+timestamp+",  nonce="+nonce+",  appid="+appId);  
              
        if (!StringUtils.isNotBlank(msgSignature))  
            return;// 微信推送给第三方开放平台的消息一定是加过密的，无消息加密无法解密消息  
       
        StringBuilder sb = new StringBuilder();  
        BufferedReader in = request.getReader();  
        String line;  
        while ((line = in.readLine()) != null) {  
            sb.append(line);  
        }  
        in.close();  
        String xml = sb.toString(); //将xml变成字符串  
              
        //logger.info("读取的XML为："+xml);  
              
        WXBizMsgCrypt pc = new WXBizMsgCrypt(getSysParamMapValue(SysParamNames.wxParaComponentToken)
            		, getSysParamMapValue(SysParamNames.wxParaComponentEncodingAESKey)
            		, getSysParamMapValue(SysParamNames.wxParaComponentAppID));  
            
        try {
        	//logger.info("加解密======================================");              
        	xml = pc.decryptMsg(msgSignature, timestamp, nonce, xml);//将xml进行加密后，和sign签名码进行对比，如果正确则返回xml  
        	//logger.info("解密后:"+xml); 
        } catch (AesException e) {  
            logger.error("       错误码为: "+e.getCode() + "         错误信息为: "+e.getMessage());  
            //应该做容错处理  
        }
        
        if (appId.equals("wx570bc396a51b8ff8")){// 微信自动化测试的专用测试公众账号  
              
        	//logger.info("进入全网发布流程=================================================================================");  
            Document doc = DocumentHelper.parseText(xml);  
            Element rootElt = doc.getRootElement();  
            String msgType = rootElt.elementText("MsgType");  
            String toUserName = rootElt.elementText("ToUserName");  
            String fromUserName = rootElt.elementText("FromUserName");  
              
            if(msgType.equals("event")){// 返回类型值，做一下区分  
                String event = rootElt.elementText("Event");  
                //返回时， 将发送人和接收人 调换一下即可   
                replyEventMessage(request,response,event,fromUserName,toUserName);  
            }  
              
            if(msgType.equals("text")){ //标示文本消息，  
                String content = rootElt.elementText("Content");  
                //返回时， 将发送人和接收人 调换一下即可   
                processTextMessage(request,response,content,fromUserName,toUserName);//用文本消息去拼接字符串。微信规定  
            }  
              
        }else{  
        	//logger.info("=================================================================================appId:" + appId);  
        	try{
        		IndexComponentHelper inti = new IndexComponentHelper(model, request, appId, xml);
        		inti.Init();
        		//logger.info("=================================================================================result:" + inti.getResult());  
        		returnJSON(inti.getResult(),response);
	        }catch (Exception e) {
				// TODO: handle exception
	        	logger.info(" ------------------------------------------------------------- "+e.toString());
			}
            //logger.info("appid="+appId+",正确的值为：wx570bc396a51b8ff8");  
            //logger.info("检测不是微信开放平台测试账号,发布程序终止.");  
        }  
          
    }  
      
    /** 
     * 方法描述: 类型为enevt的时候，拼接 
     * @param request 
     * @param response 
     * @param event 
     * @param toUserName  发送接收人 
     * @param fromUserName  发送人 
     */  
    public void replyEventMessage(HttpServletRequest request, HttpServletResponse response, String event, String toUserName, String fromUserName) throws DocumentException, IOException {  
        String content = event + "from_callback";  
        replyTextMessage(request,response,content,toUserName,fromUserName);  
    }  
      
      
    /** 
     * 方法描述: 立马回应文本消息并最终触达粉丝 
     * @param content  文本 
     * @param toUserName  发送接收人 
     * @param fromUserName  发送人 
     */  
    public void processTextMessage(HttpServletRequest request, HttpServletResponse response, String content,String toUserName, String fromUserName) throws IOException, DocumentException{  
        if("TESTCOMPONENT_MSG_TYPE_TEXT".equals(content)){  
            String returnContent = content+"_callback";  
            replyTextMessage(request,response,returnContent,toUserName,fromUserName);  
        }else if(StringUtils.startsWithIgnoreCase(content, "QUERY_AUTH_CODE")){  
            response.getWriter().print("");//需在5秒内返回空串表明暂时不回复，然后再立即使用客服消息接口发送消息回复粉丝  
            logger.info("content:"+content+" content[1]:"+content.split(":")[1]+" fromUserName:"+fromUserName+" toUserName:"+toUserName);  
            //接下来客服API再回复一次消息  
            //此时 content字符的内容为是 QUERY_AUTH_CODE:adsg5qe4q35  
            replyApiTextMessage(content.split(":")[1],toUserName);  
        }  
    }  
      
      
      
    /** 
     * 方法描述: 直接返回给微信开放平台 
     * @param request 
     * @param response 
     * @param content  文本 
     * @param toUserName  发送接收人 
     * @param fromUserName  发送人 
     */  
    public void replyTextMessage(HttpServletRequest request, HttpServletResponse response, String content,String toUserName, String fromUserName) throws DocumentException, IOException {  
        Long createTime = System.currentTimeMillis() / 1000;  
        StringBuffer sb = new StringBuffer(512);  
        sb.append("<xml>");  
        sb.append("<ToUserName><![CDATA["+toUserName+"]]></ToUserName>");  
        sb.append("<FromUserName><![CDATA["+fromUserName+"]]></FromUserName>");  
        sb.append("<CreateTime>"+createTime.toString()+"</CreateTime>");  
        sb.append("<MsgType><![CDATA[text]]></MsgType>");  
        sb.append("<Content><![CDATA["+content+"]]></Content>");  
        sb.append("</xml>");  
        String replyMsg = sb.toString();  
        logger.info("确定发送的XML为："+replyMsg);//千万别加密  
        returnJSON(replyMsg,response);  
    }  
      
    /** 
     * 方法描述: 调用客服回复消息给粉丝 
     * @param auth_code 
     * @param fromUserName 
     * @throws DocumentException 
     * @throws IOException 
     * @return void 
     */  
    public void replyApiTextMessage(String auth_code, String fromUserName) throws DocumentException, IOException {  
            // 得到微信授权成功的消息后，应该立刻进行处理！！相关信息只会在首次授权的时候推送过来  
            String componentAccessToken= WxTokenAndJsticketCache.getComponent_access_token();//本人平台缓存的token  
            //PlatformParam component = platformParamService.selectPlatformParam();//获取 平台ID  
            //https://api.weixin.qq.com/cgi-bin/component/api_query_auth  到这个微信的接口去获取数据  
            
            String auth_url = "https://api.weixin.qq.com/cgi-bin/component/api_query_auth?component_access_token=" + componentAccessToken;
            String postJson = "{\"component_appid\":\"" + getSysParamMapValue(SysParamNames.wxParaComponentAppID) + "\",\"authorization_code\": \"" + auth_code + "\"}";
            String auth_result = HttpClientUtil.post(auth_url, postJson);
            if(StringUtils.isEmpty(auth_result))
            	return;
            JSONObject jsonObject = JSONObject.fromObject(auth_result);
            String authorizer_access_token = "";
            if (jsonObject.has("authorization_info") && jsonObject.getJSONObject("authorization_info") != null)
            {
            	authorizer_access_token = jsonObject.getJSONObject("authorization_info").getString("authorizer_access_token");
            }else {
				return;
			}
             
              
            String url = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token="+authorizer_access_token;  
            JSONObject json = new JSONObject();  
            json.put("touser",fromUserName);  
            json.put("msgtype", "text");  
            json.put("text", "{\"content\":\""+auth_code+"_from_api"+"\"}");  
              
            String result = HttpClientUtil.post(url, json.toString());  
            logger.info("客服发送接口返回值:"+result);  
    }     
      
      
    /** 
     * 方法描述: 返回数据到请求方 
     * @param data 数据 
     * @param response 
     */  
    public void returnJSON(Object data,HttpServletResponse response) {  
        try {  
            ObjectMapper objectMapper = new ObjectMapper();  
            JsonEncoding encoding = JsonEncoding.UTF8;  
            response.setContentType("application/json");  
            org.codehaus.jackson.JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(response.getOutputStream(), encoding);  
            objectMapper.writeValue(generator, data);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
      
      
    /** 
     * 方法描述: 
     * @param args 
     * @return void 
     */  
    public static void main(String[] args) {  
        JSONObject j=new JSONObject();  
        j.put("content", "aaa"+"_from_api");  
        System.out.println(j.toString());  
        System.out.println("{\"content\":\"好的_from_api\"}");  
    }  
      
    
    
}  
