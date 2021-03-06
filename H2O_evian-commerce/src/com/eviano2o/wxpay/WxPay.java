package com.eviano2o.wxpay;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviano2o.util.HttpClientUtil;
import com.eviano2o.util.XmlStringUtil;

/** 一定要注意支付的参数名和值的大小写，一定一定要注意 */
public class WxPay {
	private static final Logger logger = LoggerFactory.getLogger(WxPay.class);
	public PayResponseReturnModel Pay(WxPayModel wxPayRequest)
	{
		logger.info("=======================================================================微信JS支付开始");
		logger.info("微信JS支付入口参数："+wxPayRequest.toString());
		/** 封装参数 */
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		packageParams.put("appid", wxPayRequest.getAppId());
		packageParams.put("mch_id", wxPayRequest.getMchId());
		packageParams.put("nonce_str", wxPayRequest.getNonceStr().toLowerCase());
		packageParams.put("body", wxPayRequest.getBody());
		packageParams.put("out_trade_no", wxPayRequest.getOrderSN()); // 商户订单号
		packageParams.put("total_fee", String.valueOf(wxPayRequest.getTotalFee())); // 支付金额，这边需要转成字符串类型，否则后面的签名会失败 
		packageParams.put("spbill_create_ip", wxPayRequest.getCreateIp());
		packageParams.put("notify_url", wxPayRequest.getNotifyUrl()); // 支付成功后的回调地址
		//packageParams.put("sub_mch_id", wxPayRequest.getSubMchId());
		packageParams.put("trade_type", "JSAPI"); // 支付方式  
		packageParams.put("openid", wxPayRequest.getOpenId());
		
		RequestHandler reqHandler = new RequestHandler();
		reqHandler.init(wxPayRequest.getAppKey());
		String sign = reqHandler.createSign(packageParams);
		
		/** 封装报文 */
		String xml = "<xml>"+
			"<appid><![CDATA[" + wxPayRequest.getAppId() + "]]></appid>"+
			"<trade_type><![CDATA[JSAPI]]></trade_type>"+
			"<sign><![CDATA[" + sign + "]]></sign>"+
			"<spbill_create_ip><![CDATA[" + wxPayRequest.getCreateIp() + "]]></spbill_create_ip>"+
			"<total_fee>" + wxPayRequest.getTotalFee() + "</total_fee>"+
			"<openid><![CDATA[" + wxPayRequest.getOpenId() + "]]></openid>"+
			"<out_trade_no><![CDATA[" + wxPayRequest.getOrderSN() + "]]></out_trade_no>"+
			"<mch_id><![CDATA[" + wxPayRequest.getMchId() + "]]></mch_id>"+
			"<body><![CDATA[" + wxPayRequest.getBody() + "]]></body>"+
			"<nonce_str><![CDATA[" + wxPayRequest.getNonceStr().toLowerCase() + "]]></nonce_str>"+
			"<notify_url><![CDATA[" + wxPayRequest.getNotifyUrl() + "]]></notify_url>"+
			//"<sub_mch_id><![CDATA["+wxPayRequest.getSubMchId()+"]]></sub_mch_id>"+
			"</xml>";

		//获取预支付ID
		String createOrderURL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
		logger.info("微信支付prepayId请求地址: "+createOrderURL+", 请求数据: "+xml);
		String prepayContent = HttpClientUtil.post(createOrderURL, xml);
		logger.info("微信支付prepayId请求返回结果: "+prepayContent);
		
		Map map = new HashMap();
		try{
			map = XmlStringUtil.stringToXMLParse(prepayContent);
		}catch (Exception e){}

		String prepayId = map.containsKey("prepay_id")?map.get("prepay_id").toString():"";
		/** 封装签名参数 */
		String packages = "prepay_id="+prepayId;
		
		SortedMap<String, String> paySignParams = new TreeMap<String, String>();
		paySignParams.put("appId", wxPayRequest.getAppId());
		paySignParams.put("timeStamp", wxPayRequest.getTimeStamp());  
		paySignParams.put("nonceStr", wxPayRequest.getNonceStr());  
		paySignParams.put("package", packages);  
		paySignParams.put("signType", "MD5");
		
		RequestHandler paySingHandler = new RequestHandler();
		paySingHandler.init(wxPayRequest.getAppKey());
		String paySign = paySingHandler.createSign(paySignParams);
		
		PayResponseReturnModel result = new PayResponseReturnModel();
        result.setAppId(wxPayRequest.getAppId());
        result.setTimeStamp(wxPayRequest.getTimeStamp());
        result.setNonceStr(wxPayRequest.getNonceStr());
        result.setPackage(packages);
        result.setPaySign(paySign);
        result.setDanHao(wxPayRequest.getDanHao());
        result.setTotalFee(wxPayRequest.getTotalFee());
        result.setCode("E00000");
        result.setJiaMiID(wxPayRequest.getJiaMiID());
		
		/** 返回微信JS支付所需的package */
		logger.info("请求微信JS支付,返回结果：" + result.toString());
		
		logger.info("=======================================================================微信JS支付结束");
		return result;
	}
}
