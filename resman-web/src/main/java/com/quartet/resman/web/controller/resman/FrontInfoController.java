package com.quartet.resman.web.controller.resman;

import com.quartet.resman.entity.Code;
import com.quartet.resman.entity.Info;
import com.quartet.resman.service.CodeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
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

    @Autowired
    private CodeService codeService;

    @RequestMapping(value = "news")
    public String news(@RequestParam(value = "type", required = false, defaultValue = Constants.INFO_TYPE_NEWS) String type, @PageableDefault(size = 20, sort = "crtdate",
            direction = Sort.Direction.DESC) Pageable page, Model model) {
        Page<Info> news = null;
        String infoType = type;
        // news = infoService.getInfo(Constants.INFO_TYPE_NEWS,true, page);
        news = infoService.getInfo(infoType, true, page);
        model.addAttribute("news", news.getContent());
        model.addAttribute("curPage", news.getNumber());
        model.addAttribute("totalPage", news.getTotalPages());
        model.addAttribute("totalCount", news.getTotalElements());
        model.addAttribute("toptitle", getInfoCodeTypeName(infoType));
        // model.addAttribute("bannerNews", infoService.getFirstBannerInfo(Constants.INFO_TYPE_NEWS));
        model.addAttribute("bannerNews", infoService.getFirstBannerInfo(infoType));

        return "front/news";
    }

    @RequestMapping(value = "news/{id}")
    public String newsDetail(@RequestParam(value = "type", required = false, defaultValue = Constants.INFO_TYPE_NEWS) String type, @PathVariable(value = "id") Long id, Model model) {
        Info info = infoService.getInfoEager(id);
        model.addAttribute("news", info);
        String infoType = type;
        Info pre = infoService.getPreOrNextInfo(info.getId(), "pre", infoType);
        Info next = infoService.getPreOrNextInfo(info.getId(), "next", infoType);
        //   Info pre = infoService.getPreOrNextInfo(info.getId(), "pre", Constants.INFO_TYPE_NEWS);
        //  Info next = infoService.getPreOrNextInfo(info.getId(), "next", Constants.INFO_TYPE_NEWS);
        if (pre != null) {
            model.addAttribute("pre", pre);
        }
        if (next != null) {
            model.addAttribute("next", next);
        }
        infoService.updateInfoReadCount(id);
        model.addAttribute("type", infoType);
        model.addAttribute("toptitle", getInfoCodeTypeName(infoType));
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
    public String teacherDetail(@PathVariable(value = "id") Long id, Model model) {
        Info info = infoService.getInfoEager(id);
        model.addAttribute("info", info);
        Info pre = infoService.getPreOrNextInfo(info.getId(), "pre", Constants.INFO_TYPE_TEACHERGROUP);
        Info next = infoService.getPreOrNextInfo(info.getId(), "next", Constants.INFO_TYPE_TEACHERGROUP);
        if (pre != null) {
            model.addAttribute("pre", pre);
        }
        if (next != null) {
            model.addAttribute("next", next);
        }
        infoService.updateInfoReadCount(id);
        return "front/show_teachers";
    }


    @RequestMapping(value = "achievements")
    public String achievements(@PageableDefault(size = 8, sort = "crtdate",
            direction = Sort.Direction.DESC) Pageable page, Model model) {
        Page<Info> infos = infoService.getBannerInfo(Constants.INFO_TYPE_ACHIVEMENT, true, page);
        model.addAttribute("infos", infos.getContent());
        model.addAttribute("curPage", infos.getNumber());
        model.addAttribute("totalPage", infos.getTotalPages());
        model.addAttribute("totalCount", infos.getTotalElements());
        return "front/achievements";
    }

    @RequestMapping(value = "achievements/{id}")
    public String achievementsDetail(@PathVariable(value = "id") Long id, Model model) {
        Info info = infoService.getInfoEager(id);
        model.addAttribute("info", info);
        Info pre = infoService.getPreOrNextInfo(info.getId(), "pre", Constants.INFO_TYPE_ACHIVEMENT);
        Info next = infoService.getPreOrNextInfo(info.getId(), "next", Constants.INFO_TYPE_ACHIVEMENT);
        if (pre != null) {
            model.addAttribute("pre", pre);
        }
        if (next != null) {
            model.addAttribute("next", next);
        }
        infoService.updateInfoReadCount(id);
        return "front/show_achievements";
    }

    @RequestMapping(value = "knowledge/{id}")
    public String showKnowledge(@PathVariable(value = "id") Long idl, HttpServletRequest request) {
        String baseUrl = request.getContextPath();
        return "forward:" + "/front/news/" + idl + "?type=knowledge";
    }

    @RequestMapping(value = "knowledge")
    public String knowledges(HttpServletRequest request) {
        String baseUrl = request.getContextPath();
        return "redirect:" + baseUrl + "/front/news?type=knowledge";
        // return "redirect:./news?type=knowledge";
    }


    @RequestMapping(value = "studentArea")
    @ResponseBody
    public Map<String, List<?>> studentAreaData() {
        Map<String, List<?>> result = new HashMap<>();
        Pageable p = new PageRequest(0, 6, new Sort(Sort.Direction.DESC, "crtdate"));
        Page<Info> kns = infoService.getInfo(Constants.INFO_TYPE_KNOWLEDGE, true, p);
        result.put("knowledge", kns.getContent());

        p = new PageRequest(0, 6, new Sort(Sort.Direction.DESC, "crtdate"));
        Page<QuestionVo> queses = quesService.getAllQuestionVo(p);
        result.put("ques", queses.getContent());

        result.put("conn", new ArrayList());

        return result;
    }

    @RequestMapping(value = "wss")
    public ModelAndView works(String type, @PageableDefault(size = 15, sort = "crtdate",
            direction = Sort.Direction.DESC) Pageable page) {
        ModelAndView mv = new ModelAndView("front/works_skills");
        if (type == null) {
            type = Constants.INFO_TYPE_SKILL;
        }
        if (type.equalsIgnoreCase(Constants.INFO_TYPE_SKILL)) {
            Info banner = infoService.getFirstBannerInfo(type);
            if (banner != null) {
                mv.addObject("banner", banner);
            }
        }
        Page<Info> infos = infoService.getInfo(type, true, page);
        mv.addObject("type", type);
        mv.addObject("infos", infos.getContent());
        mv.addObject("curPage", infos.getNumber());
        mv.addObject("totalPage", infos.getTotalPages());
        mv.addObject("totalCount", infos.getTotalElements());
        return mv;
    }

    @RequestMapping(value = "wss/{id}")
    public ModelAndView worksDetail(@PathVariable Long id, String type) {
        ModelAndView mv = new ModelAndView("front/show_works_skills");
        Info info = infoService.getInfoEager(id);
        Info pre = infoService.getPreOrNextInfo(id, "pre", type);
        Info next = infoService.getPreOrNextInfo(id, "next", type);
        mv.addObject("type", type);

        mv.addObject("info", info);
        if (pre != null) {
            mv.addObject("pre", pre);
        }
        if (next != null) {
            mv.addObject("next", next);
        }
        System.out.print(info.getContent());
        infoService.updateInfoReadCount(id);
        return mv;
    }


    @RequestMapping(value = "infoDetail")
    public String majorInfo(String type, Model model) {
        Page<Info> infos = infoService.getInfo(type,
                new PageRequest(0, 1, new Sort(Sort.Direction.DESC, "crtdate")));
        List<Info> data = infos.getContent();
        Info info = data != null && data.size() > 0 ? data.get(0) : null;
        model.addAttribute("type", type);
        if (info != null) {
            model.addAttribute("info", info);
            infoService.updateInfoReadCount(info.getId());
        }
        return "front/show_major_contact";
    }

    public String getInfoCodeTypeName(String _type) {
        String name = "";
        Code code = codeService.getInfoCode(_type);
        if (code != null)
            name = code.getName();
        return name;
    }

}
