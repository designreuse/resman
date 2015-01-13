package com.quartet.resman.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 参数服务
 * User: qfxu
 * Date: 15-1-13
 */
@Service
public class ParamServcie {

    //个人空间根目录
    @Value("${folder.personal}")
    private String folderPersonal;

    public String getFolderPersonal() {
        return folderPersonal;
    }
}
