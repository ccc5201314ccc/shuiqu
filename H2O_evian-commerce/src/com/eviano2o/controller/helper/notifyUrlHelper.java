package com.eviano2o.controller.helper;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.xml.sax.InputSource;

import com.alibaba.druid.util.StringUtils;
import com.eviano2o.bean.weixin.WxPayNotifyModel;
import com.eviano2o.service.WeiXinService;
import com.eviano2o.util.DateUtil;

public class notifyUrlHelper extends BaseControllerHelper {

	private static final Logger logger = LoggerFactory.getLogger(notifyUrlHelper.class);

	WeiXinService weiXinService = new WeiXinService();
	
	public notifyUrlHelper(Model model, HttpServletRequest request, HttpServletResponse response) {
		super(model, request, response);
	}

	@Override
	public void Init() {
		logger.info("微信支付回调数据开始！");
		// 示例报文
		// String xml =
		// "<xml><appid><![CDATA[wxb4dc385f953b356e]]></appid><bank_type><![CDATA[CCB_CREDIT]]></bank_type><cash_fee><![CDATA[1]]></cash_fee><fee_type><![CDATA[CNY]]></fee_type><is_subscribe><![CDATA[Y]]></is_subscribe><mch_id><![CDATA[1228442802]]></mch_id><nonce_str><![CDATA[1002477130]]></nonce_str><openid><![CDATA[o-HREuJzRr3moMvv990VdfnQ8x4k]]></openid><out_trade_no><![CDATA[1000000000051249]]></out_trade_no><result_code><![CDATA[SUCCESS]]></result_code><return_code><![CDATA[SUCCESS]]></return_code><sign><![CDATA[1269E03E43F2B8C388A414EDAE185CEE]]></sign><time_end><![CDATA[20150324100405]]></time_end><total_fee>1</total_fee><trade_type><![CDATA[JSAPI]]></trade_type><transaction_id><![CDATA[1009530574201503240036299496]]></transaction_id></xml>";
		String inputLine;
		String notityXml = "";
		String resXml = "";

		try {
			while ((inputLine = _request.getReader().readLine()) != null) {
				notityXml += inputLine;
			}
			_request.getReader().close();
		} catch (Exception e) {
			e.printStackTrace();
			_result = "no";
			logger.info("解析错误：" + e.toString());
			return;
		}

		logger.info("接收到的报文：" + notityXml);
		
		if(StringUtils.isEmpty(notityXml)){
			_result = "empty";
			logger.info("接收到的报文为空");
			return;
		}

		Map m = parseXmlToList2(notityXml);
		WxPayNotifyModel wpr = new WxPayNotifyModel();
		wpr.setAppid(m.get("appid").toString());
		wpr.setBankType(m.get("bank_type").toString());
		wpr.setCashFee(m.get("cash_fee").toString());
		wpr.setFeeType(m.get("fee_type").toString());
		wpr.setIsSubscribe(m.get("is_subscribe").toString());
		wpr.setMchId(m.get("mch_id").toString());
		wpr.setNonceStr(m.get("nonce_str").toString());
		wpr.setOpenid(m.get("openid").toString());
		wpr.setOutTradeNo(m.get("out_trade_no").toString());
		wpr.setResultCode(m.get("result_code").toString());
		wpr.setReturnCode(m.get("return_code").toString());
		wpr.setSign(m.get("sign").toString());
		wpr.setTimeEnd(m.get("time_end").toString());
		wpr.setTotalFee(String.valueOf(Double.valueOf(m.get("total_fee").toString())/100.0));
		wpr.setTradeType(m.get("trade_type").toString());
		wpr.setTransactionId(m.get("transaction_id").toString());

		if ("SUCCESS".equals(wpr.getResultCode())) {
			// 支付成功
			resXml = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>" + "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
			String result = weiXinService.weixinPayToSuc(wpr.getOpenid(), wpr.getOutTradeNo(), wpr.getTransactionId(), wpr.getBankType(), wpr.getTotalFee(), DateUtil.getStringDate(),getSessionWXAppId());
			JSONObject jsonResult = JSONObject.fromObject(result);
			if(jsonResult.has("code") && jsonResult.getString("code").equals("E00000")){
				_result = "success";
			}else{
				_result = "faild1";
			}
		} else {;
			resXml = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>" + "<return_msg><![CDATA[报文为空]]></return_msg>" + "</xml> ";
			_result = "faild";
		}

		logger.error("微信支付回调数据结束！");
	}

	@Override
	public String getResult() {
		return _result;
	}

	/**
	 * description: 解析微信通知xml
	 * 
	 * @param xml
	 * @return
	 * @author ex_yangxiaoyi
	 * @see
	 */
	@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
	private static Map parseXmlToList2(String xml) {
		Map retMap = new HashMap();
		try {
			StringReader read = new StringReader(xml);
			// 创建新的输入源SAX 解析器将使用 InputSource 对象来确定如何读取 XML 输入
			InputSource source = new InputSource(read);
			// 创建一个新的SAXBuilder
			SAXBuilder sb = new SAXBuilder();
			// 通过输入源构造一个Document
			Document doc = (Document) sb.build(source);
			Element root = doc.getRootElement();// 指向根节点
			List<Element> es = root.getChildren();
			if (es != null && es.size() != 0) {
				for (Element element : es) {
					retMap.put(element.getName(), element.getValue());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retMap;
	}

}
