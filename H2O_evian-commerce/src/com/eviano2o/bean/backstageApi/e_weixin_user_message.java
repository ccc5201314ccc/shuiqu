package com.eviano2o.bean.backstageApi;

public class e_weixin_user_message {
	String openId;						// 微信id
	String message;						// 消息
	String messageType;					// 消息类型
	String recongnition;				// 认可
	Integer eid;
	public String getOpenId() {
		return openId;
	}
	public void setOpenId(String openId) {
		this.openId = openId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getRecongnition() {
		return recongnition;
	}
	public void setRecongnition(String recongnition) {
		this.recongnition = recongnition;
	}
	public Integer getEid() {
		return eid;
	}
	public void setEid(Integer eid) {
		this.eid = eid;
	}
	@Override
	public String toString() {
		return "e_weixin_user_message [openId=" + openId + ", message="
				+ message + ", messageType=" + messageType + ", recongnition="
				+ recongnition + ", eid=" + eid + "]";
	}

}
