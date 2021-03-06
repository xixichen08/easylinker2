package com.easylinker.iot.v2.controller.device;

import com.alibaba.fastjson.JSONObject;
import com.easylinker.iot.v2.model.device.Device;
import com.easylinker.iot.v2.model.user.AppUser;
import com.easylinker.iot.v2.repository.AppUserRepository;
import com.easylinker.iot.v2.repository.DeviceRepository;
import com.easylinker.iot.v2.utils.ReturnResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wwhai on 2018/1/11.
 * 用来绑定设备
 */
@Api(value = "用来绑定设备", description = "用来绑定设备", tags = "用来绑定设备")
@RestController
public class DeviceBindController {
    @Autowired
    DeviceRepository deviceRepository;
    @Autowired
    AppUserRepository appUserRepository;

    /**
     * 绑定设备
     */
    @ApiOperation(value = "绑定设备", notes = "绑定设备", httpMethod = "POST")
    @RequestMapping(value = "/device/bind")
    public JSONObject bindDevice(@RequestBody JSONObject body) {
        /**
         * 绑定设备的时候需要提供设备的分许
         */
        AppUser appUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String deviceCode = body.getString("deviceCode");
        Device device = deviceRepository.findTopByDeviceCode(deviceCode);
        if (device != null) {
            /**
             * 设备存在情况下  还要考虑  是否已经被绑定
             *
             */
            List<Device> deviceList = new ArrayList<>();
            deviceList.add(device);
            if (appUserRepository.findTopByDeviceList(deviceList) != null) {
                device.setAppUser(appUser);
                deviceRepository.save(device);
                return ReturnResult.returnResult(1, "设备绑定成功!");
            } else {
                return ReturnResult.returnResult(0, "设备已经绑定到其他账户!");
            }

        } else {
            return ReturnResult.returnResult(0, "设备不存在!");
        }
    }


    /**
     * 解除设备
     */
    @ApiOperation(value = "解除设备", notes = "解除设备", httpMethod = "POST")
    @RequestMapping(value = "/device/unBind")
    public JSONObject unBindDevice(@RequestBody JSONObject body) {

        String deviceCode = body.getString("deviceCode");
        Device device = deviceRepository.findTopByDeviceCode(deviceCode);
        if (device != null) {
            device.setAppUser(null);
            deviceRepository.save(device);
            return ReturnResult.returnResult(1, "解除绑定成功!");

        } else {
            return ReturnResult.returnResult(0, "设备不存在!");
        }
    }
}
