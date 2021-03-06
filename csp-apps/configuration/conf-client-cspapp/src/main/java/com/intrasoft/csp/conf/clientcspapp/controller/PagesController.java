package com.intrasoft.csp.conf.clientcspapp.controller;


import com.intrasoft.csp.conf.clientcspapp.context.ContextUrl;
import com.intrasoft.csp.conf.clientcspapp.model.ServiceState;
import com.intrasoft.csp.conf.clientcspapp.model.SystemInstallationState;
import com.intrasoft.csp.conf.clientcspapp.model.SystemService;
import com.intrasoft.csp.conf.clientcspapp.service.BackgroundTaskService;
import com.intrasoft.csp.conf.clientcspapp.service.InstallationService;
import com.intrasoft.csp.conf.commons.types.ContactType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@Slf4j
@SessionAttributes("gitVersion")
public class PagesController implements ContextUrl {

    @Value("${client.ui.jiralink}")
    String jiraLink;

    @Value("${client.ui.statusInterval}")
    Integer statusInterval;

    @Value("${client.ui.refreshInterval}")
    Integer refreshInterval;

    @Autowired
    InstallationService installService;

    @Autowired
    BackgroundTaskService backgroundTaskService;

    @ModelAttribute("csp_contact_types")
    public ContactType[] contactTypes() {
        return ContactType.values();
    }

    @Value("${git.build.version}")
    String buildVersion;
    @Value("${git.commit.id.abbrev}")
    String commitId;
    @Value("${git.build.time}")
    String buildTime;

    @ModelAttribute("gitVersion")
    public String gitVersion() {
        return String.format("%s/%s/%s", buildVersion, commitId, buildTime);
    }

    /*
    MAIN Pages
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index() {

        return "redirect:dashboard.html";
    }

    @RequestMapping(value = PAGE_DASHBOARD, method = RequestMethod.GET)
    public ModelAndView dashboard(Model model) {
        model = this.init(model);

        if (installService.isInstallationOngoing()) {
            model.addAttribute("cspId",installService.getState().getCspId());
        }

        model.addAttribute("internetAvailable", backgroundTaskService.isInternetAvailable());
        model.addAttribute("navHomeClassActive", "active");
        model.addAttribute("reqsCheck", backgroundTaskService.canInstallAsPerRequirements());
        model.addAttribute("asyncInterval", statusInterval);

        return new ModelAndView("pages/dashboard", "dashboard", model);
    }

    @RequestMapping(value = PAGE_INSTALL, method = RequestMethod.GET)
    public ModelAndView install(Model model) {
        model = this.init(model);

        model.addAttribute("navInstallClassActive", "active");

        if (installService.isInstallationComplete() || installService.isInstallationOngoing()) {
            return new ModelAndView("pages/install-complete", "install-complete", model);
        } else if (!backgroundTaskService.canInstallAsPerRequirements() ) {
            return new ModelAndView("redirect:/dashboard.html");
        } else {
            model.addAttribute("cspId", UUID.randomUUID().toString());
            model.addAttribute("cspRegisterApi", REST_REGISTER);
            model.addAttribute("cspRegisterFilesApi", REST_REGISTER_FILES);

            return new ModelAndView("pages/install-register", "install-register", model);
        }
    }

    @RequestMapping(value = PAGE_SMTP, method = RequestMethod.GET)
    public ModelAndView editSmtp(Model model) {

        final SystemInstallationState state = installService.getState();

        model = this.init(model);
        model.addAttribute("navInstallClassActive", "active");

        model.addAttribute("smtp", state.getSmtpDetails());
        model.addAttribute("cspId", state.getCspId());

        model.addAttribute("editSmtpApi", REST_EDIT_SMTP);

        return new ModelAndView("pages/install-smtp", "install-smtp", model);
    }


    @RequestMapping(value = PAGE_UPDATES, method = RequestMethod.GET)
    public ModelAndView updates(Model model) {
        model = this.init(model);

        model.addAttribute("navUpdatesClassActive", "active");
        final String cspId = installService.getState().getCspId();
        if (cspId != null) {
            model.addAttribute("updatesRetrieveUrl", REST_UPDATESFOUND + "/" + cspId);
        }
        return new ModelAndView("pages/updates", "updates", model);
    }

    @RequestMapping(value = PAGE_SYSTEM, method = RequestMethod.GET)
    public ModelAndView system(Model model) {
        model = this.init(model);
        model.addAttribute("navSystemClassActive", "active");
        final String cspId = installService.getState().getCspId();
        if (cspId != null) {

            final List<SystemService> services = installService.queryCspServices().stream().filter(s -> s.getStartable()).collect(Collectors.toList());

            long notRunning =  services.stream().filter(s -> s.getServiceState() == ServiceState.NOT_RUNNING).count();
            long running =  services.stream().filter(s -> s.getServiceState() == ServiceState.RUNNING).count();

            log.info("Services RUNNING {} / NOT RUNNING {}", running, notRunning);

            model.addAttribute("systemRetrieveUrl", REST_MODULESERVICES + "/" + cspId);
            model.addAttribute("canStart", notRunning >= 0 && running == 0);
            model.addAttribute("canStop", running > 0);

            model.addAttribute("startUrl", PAGE_STARTMODULES + "/" + cspId);
            model.addAttribute("stopUrl", PAGE_STOPMODULES + "/" + cspId);

        }
        return new ModelAndView("pages/system", "system", model);
    }

    @RequestMapping(value = PAGE_LINKS, method = RequestMethod.GET)
    public ModelAndView getPageLinks(Model model) {
        model = this.init(model);

        //TODO if installation is complete, add the links here but when is it complete?


        return new ModelAndView("fragments :: links", "links", model);
    }

    @RequestMapping(value = PAGE_STATUS, method = RequestMethod.GET)
    public ModelAndView status(Model model) {
        model = this.init(model);

        model.addAttribute("navUpdatesClassActive", "active");
    //TODO find the inprogress task or remove these attributes
        model.addAttribute("refreshInterval", refreshInterval);
        model.addAttribute("logUrl", REST_LOG);

        return new ModelAndView("pages/status", "status", model);
    }


    @RequestMapping(value = PAGE_DOWNLOADMODULE + "/{hash}", method = RequestMethod.GET)
    public String downloadModule(@PathVariable String hash) {
        backgroundTaskService.scheduleDownload(installService.queryModuleByHash(hash));
        return "redirect:"+PAGE_STATUS;
    }


    @RequestMapping(value = PAGE_INSTALLMODULE + "/{hash}", method = RequestMethod.GET)
    public String installModule(@PathVariable String hash) {
        backgroundTaskService.scheduleInstall(installService.queryModuleByHash(hash));
        return "redirect:"+PAGE_STATUS;
    }


    @RequestMapping(value = PAGE_REINSTALLMODULE + "/{hash}", method = RequestMethod.GET)
    public String reInstallModule(@PathVariable String hash) {
        backgroundTaskService.scheduleReInstall(installService.queryModuleByHash(hash));
        return "redirect:"+PAGE_STATUS;
    }

    @RequestMapping(value = PAGE_DELETEMODULE + "/{hash}", method = RequestMethod.GET)
    public String deleteModule(@PathVariable String hash) {
        backgroundTaskService.scheduleDelete(installService.queryModuleByHash(hash));
        return "redirect:"+PAGE_STATUS;
    }


    @RequestMapping(value = PAGE_STARTMODULES + "/{cspId}", method = RequestMethod.GET)
    public String startActiveModules(@PathVariable String cspId) {
        backgroundTaskService.scheduleStartActiveModules();
        return "redirect:"+PAGE_STATUS;
    }

    @RequestMapping(value = PAGE_STOPMODULES + "/{cspId}", method = RequestMethod.GET)
    public String stopActiveModules(@PathVariable String cspId) {
        backgroundTaskService.scheduleStopActiveModules();
        return "redirect:"+PAGE_STATUS;
    }



    /*
    Internal methods
     */
    private Model init(Model m) {
        m.addAttribute("dashboardUrl", PAGE_DASHBOARD);
        m.addAttribute("installUrl", PAGE_INSTALL);
        m.addAttribute("installSmtpUrl", PAGE_SMTP);
        m.addAttribute("updatesUrl", PAGE_UPDATES);
        m.addAttribute("systemUrl", PAGE_SYSTEM);
        m.addAttribute("statusUrl", PAGE_STATUS);
        m.addAttribute("contactUrl", jiraLink);
        m.addAttribute("dashboardLinks", PAGE_LINKS);
        m.addAttribute("dashboardStatusUrl", REST_DASHSTATUS);

        m.addAttribute("startModulesUrl", PAGE_STARTMODULES);
        m.addAttribute("stopModulesUrl", PAGE_STOPMODULES);
        m.addAttribute("systemRetrieveUrl", REST_MODULESERVICES);


        m.addAttribute("navHomeClassActive", "");
        m.addAttribute("navInstallClassActive", "");
        m.addAttribute("navUpdatesClassActive", "");
        m.addAttribute("navSystemClassActive", "");

        return m;
    }
}
