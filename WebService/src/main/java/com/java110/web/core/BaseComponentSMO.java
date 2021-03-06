package com.java110.web.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.common.cache.MappingCache;
import com.java110.common.constant.CommonConstant;
import com.java110.common.constant.MappingConstant;
import com.java110.common.constant.ResponseConstant;
import com.java110.common.constant.ServiceConstant;
import com.java110.common.exception.SMOException;
import com.java110.common.factory.ApplicationContextFactory;
import com.java110.common.util.Assert;
import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.core.context.IPageData;
import com.java110.web.smo.impl.LoginServiceSMOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;

/**
 * Created by wuxw on 2019/3/22.
 */
public class BaseComponentSMO extends BaseServiceSMO {

    private static Logger logger = LoggerFactory.getLogger(BaseComponentSMO.class);

    /**
     * 调用组件
     *
     * @param componentCode   组件编码
     * @param componentMethod 组件方法
     * @param pd
     * @return
     */
    protected ResponseEntity<String> invokeComponent(String componentCode, String componentMethod, IPageData pd) {

        logger.debug("开始调用组件：{}", pd.toString());

        ResponseEntity<String> responseEntity = null;

        Object componentInstance = ApplicationContextFactory.getBean(componentCode);

        Assert.notNull(componentInstance, "未找到组件对应的处理类，请确认 " + componentCode);
        try {

            Method cMethod = componentInstance.getClass().getDeclaredMethod(componentMethod, IPageData.class);

            Assert.notNull(cMethod, "未找到组件对应处理类的方法，请确认 " + componentCode + "方法：" + componentMethod);

            logger.debug("组件编码{}，组件方法{}，pd 为{}", componentCode, componentMethod, pd.toString());

            responseEntity = (ResponseEntity<String>) cMethod.invoke(componentInstance, pd);
        } catch (Exception e) {
            logger.error("调用组件失败：", e);
            responseEntity = new ResponseEntity<String>("调用组件" + componentCode + ",组件方法" + componentMethod + "失败：" + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            return responseEntity;
        }
    }


    /**
     * 获取用户信息
     *
     * @param pd
     * @param restTemplate
     * @return
     */
    protected ResponseEntity<String> getUserInfo(IPageData pd, RestTemplate restTemplate) {
        Assert.hasLength(pd.getUserId(), "用户未登录请先登录");
        ResponseEntity<String> responseEntity = null;
        responseEntity = this.callCenterService(restTemplate, pd, "", ServiceConstant.SERVICE_API_URL + "/api/query.user.userInfo?userId=" + pd.getUserId(), HttpMethod.GET);
        // 过滤返回报文中的字段，只返回name字段
        //{"address":"","orderTypeCd":"Q","serviceCode":"","responseTime":"20190401194712","sex":"","localtionCd":"","userId":"302019033054910001","levelCd":"00","transactionId":"-1","dataFlowId":"-1","response":{"code":"0000","message":"成功"},"name":"996icu","tel":"18909780341","bId":"-1","businessType":"","email":""}

        return responseEntity;

    }

    /**
     * 查询商户信息
     *
     * @return
     */
    protected ResponseEntity<String> getStoreInfo(IPageData pd, RestTemplate restTemplate) {
        Assert.hasLength(pd.getUserId(), "用户未登录请先登录");
        ResponseEntity<String> responseEntity = null;
        responseEntity = this.callCenterService(restTemplate, pd, "", ServiceConstant.SERVICE_API_URL + "/api/query.store.byuser?userId=" + pd.getUserId(), HttpMethod.GET);

        return responseEntity;
    }

    /**
     * 查询商户信息
     *
     * @return
     */
    protected void checkStoreEnterCommunity(IPageData pd, String storeId, String storeTypeCd, String communityId, RestTemplate restTemplate) {
        Assert.hasLength(pd.getUserId(), "用户未登录请先登录");
        ResponseEntity<String> responseEntity = null;
        responseEntity = this.callCenterService(restTemplate, pd, "",
                ServiceConstant.SERVICE_API_URL + "/api/query.myCommunity.byMember?memberId=" + storeId + "&memberTypeCd="
                        + MappingCache.getValue(MappingConstant.DOMAIN_STORE_TYPE_2_COMMUNITY_MEMBER_TYPE, storeTypeCd), HttpMethod.GET);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new SMOException(ResponseConstant.RESULT_CODE_ERROR, "还未入驻小区，请先入驻小区");
        }

        Assert.jsonObjectHaveKey(responseEntity.getBody().toString(), "communitys", "还未入驻小区，请先入驻小区");

        JSONObject community = JSONObject.parseObject(responseEntity.getBody().toString());

        JSONArray communitys = community.getJSONArray("communitys");

        if (communitys == null || communitys.size() == 0) {
            throw new SMOException(ResponseConstant.RESULT_CODE_ERROR, "还未入驻小区，请先入驻小区");
        }

        JSONObject currentCommunity = getCurrentCommunity(communitys, communityId);

        if (currentCommunity == null) {
            throw new SMOException(ResponseConstant.RESULT_CODE_ERROR, "传入小区ID非法，请正常操作");
        }

    }

    private JSONObject getCurrentCommunity(JSONArray communitys, String communityId) {
        for (int communityIndex = 0; communityIndex < communitys.size(); communityIndex++) {
            if (communityId.equals(communitys.getJSONObject(communityIndex).getString("communityId"))) {
                return communitys.getJSONObject(communityIndex);
            }
        }

        return null;
    }

    /**
     * 检查用户是否有权限
     *
     * @param pd
     * @param restTemplate
     * @param privilegeCode
     */
    protected void checkUserHasPrivilege(IPageData pd, RestTemplate restTemplate, String privilegeCode) {
        ResponseEntity<String> responseEntity = null;
        responseEntity = this.callCenterService(restTemplate, pd, "", ServiceConstant.SERVICE_API_URL + "/api/check.user.hasPrivilege?userId=" + pd.getUserId() + "&pId=" + privilegeCode, HttpMethod.GET);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new SMOException(1999, "用户没有权限操作权限" + privilegeCode);
        }
    }
}
