package com.quartet.resman.web.controller.resman;

import com.quartet.resman.entity.Info;
import com.quartet.resman.service.InfoService;
import com.quartet.resman.service.QuestionService;
import com.quartet.resman.utils.Constants;
import com.quartet.resman.vo.QuestionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lcheng on 2015/4/19.
 */
@Controller
@RequestMapping("/front")
public class FrontInfoController {

    @Autowired
    private InfoService infoService;
    @Autowired
    private QuestionService quesService;


    @RequestMapping(value = "news")
    public String news(@PageableDefault(size = 20, sort = "crtdate",
            direction = Sort.Direction.DESC) Pageable page, Model model) {
        Page<Info> news = infoService.getInfo(Constants.INFO_TYPE_NEWS, true, page);
        model.addAttribute("news", news.getContent());
        model.addAttribute("curPage", news.getNumber());
        model.addAttribute("totalPage", news.getTotalPages());
        model.addAttribute("totalCount", news.getTotalElements());
        model.addAttribute("bannerNews",infoService.getFirstBannerInfo(Constants.INFO_TYPE_NEWS));
        return "front/news";
    }

    @RequestMapping(value = "news/{id}")
    public String newsDetail(@PathVariable(value = "id") Long id,Model model) {
        Info info = infoService.getInfoEager(id);
        model.addAttribute("news",info);
        Info pre = infoService.getPreOrNextInfo(info.getId(),"pre",Constants.INFO_TYPE_NEWS);
        Info next = infoService.getPreOrNextInfo(info.getId(),"next",Constants.INFO_TYPE_NEWS);
        if (pre!=null){
            model.addAttribute("pre",pre);
        }
        if (next!=null){
            model.addAttribute("next",next);
        }
        infoService.updateInfoReadCount(id);
        return "front/show_news";
    }

    @RequestMapping(value = "teachers")
    public String teachers(@PageableDefault(size = 8, sort = "crtdate",
            direction = Sort.Direction.DESC) Pageable page, Model model) {
        Page<Info> infos = infoService.getBannerInfo(Constants.INFO_TYPE_TEACHERGROUP, true, page);
        model.addAttribute("infos", infos.getContent());
        model.addAttribute("curPage", infos.getNumber());
        model.addAttribute("totalPage", infos.getTotalPages());
        model.addAttribute("totalCount", infos.getTotalElements());
        return "front/teachers";
    }

    @RequestMapping(value = "teachers/{id}")
    public String teacherDetail(@PathVariable(value = "id") Long id,Model model) {
        Info info = infoService.getInfoEager(id);
        model.addAttribute("info",info);
        Info pre = infoService.getPreOrNextInfo(info.getId(),"pre",Constants.INFO_TYPE_TEACHERGROUP);
        Info next = infoService.getPreOrNextInfo(info.getId(),"next",Constants.INFO_TYPE_TEACHERGROUP);
        if (pre!=null){
            model.addAttribute("pre",pre);
        }
        if (next!=null){
            model.addAttribute("next",next);
        }
        infoService.updateInfoReadCount(id);
        return "front/show_teachers";
    }

    @RequestMapping(value = "studentArea")
    @ResponseBody
    public Map<String,List<?>> studentAreaData(){
        Map<String,List<?>> result = new HashMap<>();
        Pageable p = new PageRequest(0,6,new Sort(Sort.Direction.DESC,"crtdate"));
        Page<Info> kns = infoService.getInfo(Constants.INFO_TYPE_KNOWLEDGE,true,p);
        result.put("knowledge", kns.getContent());

        p = new PageRequest(0,6,new Sort(Sort.Direction.DESC,"crtdate"));
        Page<QuestionVo> queses = quesService.getAllQuestionVo(p);
        result.put("ques", queses.getContent());

        result.put("conn",new ArrayList());

        return result;
    }
}
