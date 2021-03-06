package com.eviano2o.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.eviano2o.bean.weixin.ClientModel;
import com.eviano2o.bean.weixin.WxCityInfoModel;
import com.eviano2o.service.BackstageApiService;
import com.eviano2o.service.WeiXinService;
import com.eviano2o.util.SessionConstantDefine;
import com.eviano2o.util.response.JsonModel;
import com.eviano2o.util.response.JsonResponseHelper;

/**
 *如果拦截器不起效，请检查项目XML中是否有相关配置 
 */
public class CheckWeiXinLoginFilter extends HandlerInterceptorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(CheckWeiXinLoginFilter.class);

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		// request.getSession().setAttribute("username", "upxiaofeng");
		// request.getSession().setAttribute("password", "upxiaofeng");
		// request.getSession().setAttribute("userid", "10086");

	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub
		// super.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("gbk");
		response.setHeader("contentType", "application/json;charset=gbk");
		
		// TODO Auto-generated method stub
		if (handler.getClass().isAssignableFrom(HandlerMethod.class)) {
			CheckWeiXinLogin checkWeiXinLogin = ((HandlerMethod) handler).getMethodAnnotation(CheckWeiXinLogin.class);

			if (checkWeiXinLogin == null || checkWeiXinLogin.validate() == false)
				return true;

			//logger.info("1111111111111   "+(request.getSession().getAttribute(SessionConstantDefine.CLIENT_INFO) == null) + "    "+request.getSession().getAttribute(SessionConstantDefine.CLIENT_INFO).toString());
			//logger.info("2222222222222   "+((ClientModel)request.getSession().getAttribute(SessionConstantDefine.CLIENT_INFO)).getIdentityCode());
			// 在这里实现自己的权限验证逻辑
			if (request.getSession().getAttribute(SessionConstantDefine.CLIENT_INFO) == null || StringUtils.isEmpty(((ClientModel)request.getSession().getAttribute(SessionConstantDefine.CLIENT_INFO)).getClientId())) {// 如果验证成功返回true（这里直接写false来模拟验证失败的处理）

				//从数据库查询是否有对应的sessionId信息
				/*String sessionId=request.getSession().getId();
				String getOpenIdBySessionIdJson = new BackstageApiService().GetOpenIdBySessionId(sessionId);
				if(!StringUtils.isEmpty(getOpenIdBySessionIdJson)){
					JSONObject json = JSONObject.fromObject(getOpenIdBySessionIdJson);
					if(json != null && json.has("code") && json.getString("code").equals("E00000")){
						request.getSession().setAttribute(SessionConstantDefine.WX_OPENID, json.getJSONObject("data").getString("openId"));
						
						ClientModel userInfo = new WeiXinService().getWxLogin(json.getJSONObject("data").getString("openId"));
						if(userInfo==null){
							request.getSession().removeAttribute(SessionConstantDefine.CLIENT_INFO);
						}else{
							request.getSession().setAttribute(SessionConstantDefine.CLIENT_INFO, userInfo);

							return true;
						}
					}
				}*/
				
				
				logger.error("[url:{}]", new Object[] { currentUrl(request) });
				//这句话的意思，是让浏览器用utf8来解析返回的数据  
				response.setHeader("Content-type", "text/html;charset=UTF-8");  
				//这句话的意思，是告诉servlet用UTF-8转码，而不是用默认的ISO8859  
				response.setCharacterEncoding("UTF-8");
				// response.sendRedirect("account/login");
				JsonModel weiXinNoJsonModel = JsonResponseHelper.getWeiXinNoJsonModel();
				weiXinNoJsonModel.setData(JSONObject.fromObject("{\"appid\":\""+request.getSession().getAttribute("appId")+"\"}"));
				logger.info("Login = "+weiXinNoJsonModel.toString());
				response.getWriter().write(weiXinNoJsonModel.toString());
				response.getWriter().flush();
				return false;
			} else// 如果验证失败
			{
				// 返回到登录界面
				return true;
			}

		} else
			return true;
	}

	
	/** 当前网址 */
	private String currentUrl(HttpServletRequest request){
		String url = request.getRequestURL().toString();
		if(request.getQueryString()!=null) //判断请求参数是否为空  
			url+="?"+request.getQueryString();   // 参数
		return url;
	}
}
