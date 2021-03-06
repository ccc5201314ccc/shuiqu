package com.eviano2o.controller.helper;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;

import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import com.eviano2o.bean.weixin.BestDiscountActivityShoppingModel;
import com.eviano2o.bean.weixin.QuickShoppingGoods;
import com.eviano2o.bean.weixin.QuickShoppingListModel;
import com.eviano2o.bean.weixin.WxProductVouchers;
import com.eviano2o.bean.weixin.WxSaveOrderInvoiceModel;
import com.eviano2o.bean.weixin.WxSaveOrderModel;
import com.eviano2o.bean.weixin.WxSaveOrderProductModel;
import com.eviano2o.bean.weixin.WxUserAddressModel;
import com.eviano2o.util.DateUtil;
import com.eviano2o.util.DoubleFormatUtil;
import com.eviano2o.util.SettlementTypeDefine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ShoppingCarSaveHelper extends BaseControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(ShoppingCarSaveHelper.class);
	
	public ShoppingCarSaveHelper(Model model, HttpServletRequest request) {
		super(model, request);
	}
	
	@Override
	public void Init() {
		String quickSpJson = _request.getParameter("quickSpJson");
		logger.info(quickSpJson);
		if(StringUtils.isEmpty(quickSpJson)){
			//_model.addAttribute("msg", "购物车中没有商品");
			_result = formatJsonResult("E90000", "购物车中没有商品");
			return;
		}

		//用户提交的商品
		List<QuickShoppingListModel> userSubmitSp = new Gson().fromJson(quickSpJson, new TypeToken<List<QuickShoppingListModel>>() {}.getType());
		if(userSubmitSp == null || userSubmitSp.size() == 0){
			//_model.addAttribute("msg", "购物车中没有商品!");
			logger.error("（1）QuickShoppingSaveHelper："+quickSpJson);
			_result = formatJsonResult("E90000", "购物车中没有商品!");
			return;
		}
		//用户数据保存的商品
		List<QuickShoppingListModel> carSp =weiXinService.getShopCartProducts(getSessionClientIdentityCode(), 0,getSessionWXAppId());
		if(carSp == null || carSp.size() == 0){
			//_model.addAttribute("msg", "没有快速订购商品!");
			_result = formatJsonResult("E90000", "购物车中没有商品!!");
			return;
		}
		checkUserSubmitSp(userSubmitSp, carSp);
	}
	
	private void checkUserSubmitSp(List<QuickShoppingListModel> userSubmitSp, List<QuickShoppingListModel> carSp){
		//Double totalMoney = 0.00;//总金额
		//Integer totalTicket = 0;//总水票
		Hashtable<Integer, BestDiscountActivityShoppingModel> activityDiscount = new Hashtable<Integer, BestDiscountActivityShoppingModel>();//首桶半价或者首桶免费折扣金额
		List<WxSaveOrderModel> userCheckSp = new ArrayList<WxSaveOrderModel>();
		List<WxUserAddressModel> addressList = weiXinService.getUserAddressList(getSessionClientIdentityCode(),getSessionWXAppId());
		if(addressList == null || addressList.size() == 0){
			_model.addAttribute("msg", " 请先添加地址!");
			_result = formatJsonResult("E90000", " 请先添加地址!");
			return;
		}
		
		//电子票计算，防止同一商品在不同的店铺购买，加起来的数量超出用户的电子票数量
		Hashtable<Integer, Integer> checkDianZiPiaoNum = new Hashtable<Integer, Integer>();
		
		for(QuickShoppingListModel ssp : userSubmitSp){
			for(QuickShoppingListModel qsp : carSp){
				Double totalShopMoney = 0.00;
				Integer totalShopTicket = 0;
				List<WxSaveOrderProductModel> carOrderShopSP = new ArrayList<WxSaveOrderProductModel>();
				String code_no = "none";
				Boolean isUseCode = false;
				double discountMoney = 0.00;//用作水店优惠记录
				WxProductVouchers spVouchers = new WxProductVouchers();
				if(ssp.getShopId().intValue() == qsp.getShopId().intValue()){

					for(QuickShoppingGoods shopSSP : ssp.getProducts()){
						for(QuickShoppingGoods shopQSP : qsp.getProducts()){
							shopQSP.setFpid(shopSSP.getFpid());
							shopQSP.setMaxnum(shopSSP.getMaxnum());
							if(shopSSP.getPid().intValue() == shopQSP.getPid().intValue()){
								shopQSP.setBuyType(shopSSP.getBuyType());
								
								double discountSpMoney = 0.00;//用作当前商品优惠记录

								if(shopSSP.getActivityId() != null && shopSSP.getActivityId().intValue() == 11 || shopSSP.getActivityId().intValue() == 12){
									if(shopSSP.getBuyType().intValue() == 1)
									{
										if(!StringUtils.isEmpty(shopSSP.getCode_no())){
											_model.addAttribute("msg", shopQSP.getPname() + " 不能参与平台优惠活动同时使用该优惠券!");
											_result = formatJsonResult("E90000", shopQSP.getPname() + " 不能参与平台优惠活动同时使用该优惠券!");
											return;
										}
										
										if(activityDiscount.size() > 0 && !activityDiscount.containsKey(ssp.getShopId())){
											_model.addAttribute("msg", "平台优惠活动最多只能有一种商品参与!");
											_result = formatJsonResult("E90000", "平台优惠活动最多只能有一种商品参与!");
											return;
										}
										
										Double curDiscount = shopSSP.getActivityId().intValue() == 11 ? shopQSP.getVipPrice() : shopQSP.getVipPrice() / 2;
										if(!activityDiscount.containsKey(ssp.getShopId()) || curDiscount > activityDiscount.get(ssp.getShopId()).getDiscount()){
											BestDiscountActivityShoppingModel discountEntity = new BestDiscountActivityShoppingModel();
											discountEntity.setShopId(ssp.getShopId());
											discountEntity.setPid(shopSSP.getPid());
											discountEntity.setDiscount(curDiscount);
											activityDiscount.put(ssp.getShopId(), discountEntity);
										}
									}else{
										_model.addAttribute("msg", shopQSP.getPname() + " 结算方式不能参与平台优惠活动!");
										_result = formatJsonResult("E90000", "结算方式不能参与平台优惠活动!");
										return;
									}
								}

								/*if(shopSSP.getBuyType().intValue() == 2)
								{
									if(shopQSP.getSurplusNum() == 0){
										_model.addAttribute("msg", shopQSP.getPname() + " 选购数量超出已有电子票数量.");
										_result = formatJsonResult("E90000", "选购数量超出已有电子票数量.");
										return;
									}
									
									if(checkDianZiPiaoNum.contains(shopQSP.getPid())){
										if(shopSSP.getNumber().intValue() + checkDianZiPiaoNum.get(shopQSP.getPid()).intValue() > shopQSP.getSurplusNum().intValue()){
											_model.addAttribute("msg", shopQSP.getPname() + " 选购数量超出已有电子票数量!!");
											_result = formatJsonResult("E90000", "选购数量超出已有电子票数量!!");
											return;
										}
										
										checkDianZiPiaoNum.put(shopQSP.getPid(), shopSSP.getNumber().intValue() + checkDianZiPiaoNum.get(shopQSP.getPid()).intValue());
									}else{
										if(shopSSP.getNumber().intValue() > shopQSP.getSurplusNum().intValue()){
											_model.addAttribute("msg", shopQSP.getPname() + " 选购数量超出已有电子票数量!");
											_result = formatJsonResult("E90000", "选购数量超出已有电子票数量!");
											return;
										}
										
										checkDianZiPiaoNum.put(shopQSP.getPid(), shopSSP.getNumber().intValue());
									}
								}*/
								
								if(shopSSP.getBuyType().intValue() == 3 && shopQSP.getIfTicket() == false)
								{
									_model.addAttribute("msg", shopQSP.getPname() + " 结算方式不能是水票结算!");
									_result = formatJsonResult("E90000", "结算方式不能是水票结算!");
									return;
								}
								
								
								//if(shopSSP.getNumber().intValue() > shopQSP.getRepertoryNum().intValue()){
								//	_model.addAttribute("msg", shopQSP.getPname() + " 数量超过库存!");
								//	_result = formatJsonResult("E90000", shopQSP.getPname() + " 数量超过库存!");
								//	return;
								//}
								
								double curSPAllMoney = shopQSP.getNumber() * shopQSP.getVipPrice();//商品金额,减去优惠券金额（如果有优惠券）
								if(shopSSP.getBuyType().intValue() == 1 && !StringUtils.isEmpty(shopSSP.getCode_no())){
									if(shopSSP.getActivityId() > 0){
										_model.addAttribute("msg", shopQSP.getPname() + " 不能使用该优惠券同时参与平台优惠活动!");
										_result = formatJsonResult("E90000", shopQSP.getPname() + " 不能使用该优惠券同时参与平台优惠活动!");
										return;
									}
									if(isUseCode){//如果同一水店已经用过优惠券
										_model.addAttribute("msg", shopQSP.getPname() + " 同一个水店只能使用一张优惠券!");
										_result = formatJsonResult("E90000", shopQSP.getPname() + " 同一个水店只能使用一张优惠券!");
										return;
									} else {
										isUseCode = true;
										code_no = shopSSP.getCode_no();
										spVouchers = getProductVoucher(shopSSP.getPid(), code_no);
										if(spVouchers == null)
										{
											_model.addAttribute("msg", shopQSP.getPname() + " 查询不到该优惠券!");
											_result = formatJsonResult("E90000", shopQSP.getPname() + " 查询不到该优惠券!");
											return;
										}
										if(curSPAllMoney > spVouchers.getConvert_money()){
											curSPAllMoney = DoubleFormatUtil.sub(curSPAllMoney, spVouchers.getConvert_money());
											discountMoney = spVouchers.getConvert_money();
											discountSpMoney = spVouchers.getConvert_money();
										}
										else{
											discountMoney = curSPAllMoney;
											discountSpMoney  = curSPAllMoney;
											curSPAllMoney = 0.00;
										}
										shopQSP.setConvert_money(spVouchers.getConvert_money());
									}
								}else{
									code_no = "none";
									discountSpMoney = 0.00;
								}
								
								shopQSP.setIfTicket(shopSSP.getIfTicket());
								shopQSP.setNumber(shopSSP.getNumber());
								shopQSP.setFpid(shopSSP.getFpid());
								shopQSP.setMaxnum(shopSSP.getMaxnum());
								
								if(shopSSP.getBuyType().intValue() == 1){
									//totalMoney = DoubleFormatUtil.sum(totalMoney, curSPAllMoney);
									totalShopMoney  = DoubleFormatUtil.sum(totalShopMoney, curSPAllMoney);
								}
								
								if(shopSSP.getBuyType().intValue() == 3){
									//totalTicket += shopQSP.getNumber();
									totalShopTicket += shopQSP.getNumber();
								}
								
								if(shopSSP.getBuyType().intValue() == 4){
									//totalMoney = DoubleFormatUtil.sum(totalMoney, curSPAllMoney);
									totalShopMoney  = DoubleFormatUtil.sum(totalShopMoney, curSPAllMoney);
								}
								
								WxSaveOrderProductModel curSp = new WxSaveOrderProductModel();
								curSp.setPid(shopQSP.getPid());
								curSp.setNumber(shopQSP.getNumber());
								curSp.setPrice(shopQSP.getVipPrice());
								//curSp.setTotal(shopSSP.getBuyType() == 1 ? shopQSP.getNumber() * shopQSP.getVipPrice() : 0.00);
								curSp.setTotal(shopQSP.getNumber() * shopQSP.getVipPrice());
								curSp.setSettlementType(SettlementTypeDefine.getDefine().get(shopSSP.getBuyType()));
								curSp.setVoucherCode(code_no.equals("none") ? "" : code_no);
								curSp.setFpid(shopSSP.getFpid());
								curSp.setMaxnum(shopSSP.getMaxnum());
								if(shopSSP.getActivityId() != null && (shopSSP.getActivityId().intValue() == 11 || shopSSP.getActivityId().intValue() == 12)){
									curSp.setVoucherMoney(activityDiscount.get(ssp.getShopId()).getDiscount());
								}else{
									curSp.setVoucherMoney(discountSpMoney);
								}
								curSp.setActivityId(shopSSP.getActivityId());
								curSp.setFpid(shopSSP.getFpid());
								curSp.setMaxnum(shopSSP.getMaxnum());
								carOrderShopSP.add(curSp);
							}
						}
					}
				}
				
				if(carOrderShopSP.size() > 0){
					
					if(activityDiscount.size() == 1){
						for(WxSaveOrderProductModel sp : carOrderShopSP){
							if(activityDiscount.containsKey(ssp.getShopId()) && sp.getPid().intValue() == activityDiscount.get(ssp.getShopId()).getPid()){
								sp.setVoucherMoney(activityDiscount.get(ssp.getShopId()).getDiscount());
							}
						}
					}
					
					WxUserAddressModel curAddress = null;
					for(WxUserAddressModel address : addressList){
						if(address.getDid().intValue() == ssp.getDid().intValue())
							curAddress = address;
					}
					
					if(curAddress == null){
						_result = formatJsonResult("E90000", "地址错误!");
						return;
					}
					
					String appointmentTime = checkYuYueTime(ssp.getAppointmentTime());
					if(StringUtils.isEmpty(appointmentTime)){
						_result = formatJsonResult("E90000", "预约时间错误!");
						return;
					}
					if(activityDiscount.containsKey(ssp.getShopId()))
						totalShopMoney = DoubleFormatUtil.sub(totalShopMoney, activityDiscount.get(ssp.getShopId()).getDiscount());//减去平台优惠价格
					totalShopMoney = DoubleFormatUtil.sum(totalShopMoney, qsp.getFreight());//加上运费
					WxSaveOrderModel curShop = new WxSaveOrderModel();
					curShop.setShopId(qsp.getShopId());
					curShop.setDid(curAddress.getDid());
					curShop.setSendAddress(curAddress.getStreetDescribe() + curAddress.getDoorplate());
					curShop.setPhone(curAddress.getPhone());
					curShop.setContacts(curAddress.getContacts());
					curShop.setAppointmentTime(appointmentTime);
					curShop.setSendRemark(ssp.getSendRemark());
					curShop.setSdkType("3");
					curShop.setMobileIMEL(getSessionWeiXinId());
					curShop.setMobileType("微信");
					curShop.setAppVer("微信");
					curShop.setOrderRemark(ssp.getOrderRemark());
					curShop.setTicketCount(totalShopTicket);
					curShop.setReceivableTotal(totalShopMoney);
					curShop.setCashTotal(0.0);//无效
					curShop.setLinePayTotal(0.0);//无效
					curShop.setPayMode(ssp.getPayMode());
					curShop.setPrivilegeId(ssp.getPrivilegeId());
					curShop.setActivityType(0);
					curShop.setCode_no(code_no.equals("none") ? "" : code_no);
					curShop.setCode_money(spVouchers.getConvert_money());
					curShop.setDiscountMoney(discountMoney);
					if(activityDiscount.size() > 0 && activityDiscount.containsKey(ssp.getShopId())){
						curShop.setDiscountMoney(activityDiscount.get(ssp.getShopId()).getDiscount());
					}else{
						curShop.setDiscountMoney(discountMoney);
					}
					curShop.setDiscountDescribe("");
					curShop.setFreight(qsp.getFreight());
					curShop.setIntegral(0);
					curShop.setGoodsJson(carOrderShopSP);
					if(ssp.getInvoiceType() != null && (ssp.getInvoiceType().intValue() == 0 
							|| ssp.getInvoiceType().intValue() == 1 ||ssp.getInvoiceType().intValue()==2)){
						WxSaveOrderInvoiceModel invoice = new WxSaveOrderInvoiceModel();
						invoice.setType(ssp.getInvoiceType());
						invoice.setInvoicoName(ssp.getInvoicoName());
						invoice.setInvoiceDetail(ssp.getInvoiceDetail());
						invoice.setVatId(ssp.getVatId());
						curShop.setInvoice(invoice);
					}else{
						curShop.setInvoice(null);
					}
					
					userCheckSp.add(curShop);
				}
			}
		}
		if(userCheckSp.size() > 0){
			//_model.addAttribute("model", userCheckSp);
			//_model.addAttribute("totalMoney", totalMoney);
			//_model.addAttribute("totalTicket", totalTicket);
			//_request.getSession().setAttribute(SessionConstantDefine.QUICKCART, userCheckSp);
			
			List<BasicNameValuePair> params=new ArrayList<BasicNameValuePair>();
	    	//添加参数
	    	params.add(new BasicNameValuePair("clientId", getSessionClientIdentityCode()));
	    	//JSONObject jsonOrder=JSONObject.fromObject(userCheckSp);
	    	JSONArray jsonOrder = JSONArray.fromObject(userCheckSp);
	    	String jstr=jsonOrder.toString();
	    	params.add(new BasicNameValuePair("orderJson", jstr));
	    	//logger.info(jstr);
	    
			_result = weiXinService.saveOrder(params);
			//logger.info(_result);
		}
		else
			_result = formatJsonResult("E90000", "购物车中没有商品");
		
	}
	
	//商品选择优惠券
	private WxProductVouchers getProductVoucher(Integer pid, String code_no){
		List<WxProductVouchers> pVoucherList = weiXinService.getProductVouchers(getSessionClientIdentityCode(), 2, pid,getSessionWXAppId());
		if(pVoucherList == null || pVoucherList.size() == 0) return null;
		for(WxProductVouchers voucher : pVoucherList){
			if(voucher.getCode_no().equals(code_no))
				return voucher;
		}
		return null;
	}
	
	//预约时间判断, 提交数据格式为：今天 10:20-10:40
	private String checkYuYueTime(String submitTime){
		if(StringUtils.isEmpty(submitTime) || submitTime.split(" ").length != 2){
			logger.info(submitTime + " | " + submitTime.split(" ").length);
			return "";
		}
		String date = "";
		if(submitTime.split(" ")[0].equals("今天"))
			date = DateUtil.getUserDate("yyyy-MM-dd");
		else if (submitTime.split(" ")[0].equals("明天"))
			date = DateUtil.addDays(DateUtil.getUserDate("yyyy-MM-dd"), 1);
		else if (submitTime.split(" ")[0].equals("后天"))
			date = DateUtil.addDays(DateUtil.getUserDate("yyyy-MM-dd"), 2);
		else{
			System.out.println("22222222222222");
			return "";
		}
		
		String result = date + " " + submitTime.split(" ")[1].split("-")[1];
		
		if(DateUtil.strToDateLong(result+":00").before(DateUtil.getNow())){
			System.out.println(DateUtil.strToDateLong(result+":00")+" | " + DateUtil.getNow());
			return "";
		}
		
		return result;
	}
	
	
	@Override
	public String getResult() {
		return _result;
	}
}
