package com.pbd.project.web.controller.passenger;

import com.pbd.project.domain.*;
import com.pbd.project.domain.enums.Gender;
import com.pbd.project.domain.enums.TravelStatus;
import com.pbd.project.domain.enums.UF;
import com.pbd.project.service.healthCenter.HealthCenterService;
import com.pbd.project.service.location.LocationService;
import com.pbd.project.service.passenger.PassengerService;
import com.pbd.project.service.role.RoleService;
import com.pbd.project.service.user.UserService;
import com.pbd.project.web.validation.PassengerValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/passengers")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private UserService userService;

    @Autowired
    private HealthCenterService healthCenterService;

    @Autowired
    private PassengerValidator passengerValidator;

    @Autowired
    private RoleService roleService;

    @Autowired
    private LocationService locationService;


    @InitBinder("passenger")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(this.passengerValidator);
    }

    @GetMapping("")
    public String passengersListView(ModelMap model,
                                     @RequestParam("page") Optional<Integer> page,
                                     @RequestParam("name") Optional<String> name) {

        User user = userService.getUserAuthenticated();
        int currentPage = page.orElse(0);
        String passengerName = name.orElse(null);

        Page<Passenger> passengers = null;


        if (user.getStaff()){
            passengers = passengerName == null ?
                    passengerService.findAll(currentPage) :
                    passengerService.findPassengerByName(currentPage, passengerName);
        } else if (user.userIs("OPERADOR")) {
            passengers = passengerName == null ?
                    passengerService.findPassengerByHealthCenterAndActive(
                            currentPage,
                            user.getHealthCenter(),
                            true) :
                    passengerService.findPassengerByHealthCenterAndActiveAndNameContainsIgnoreCase(currentPage,
                            user.getHealthCenter(), true, passengerName);
        } else if (user.userIs("GESTOR")){
            passengers = passengerName == null ?
                    passengerService.findPassengerByHealthCenter(currentPage, user.getHealthCenter()) :
                    passengerService.findPassengerByHealthCenterAndNameContainsIgnoreCase(
                            currentPage,
                            user.getHealthCenter(),
                            passengerName);
        }

        model.addAttribute("passengers", passengers);
        model.addAttribute("passengerName", passengerName);
        model.addAttribute("isSearch", passengerName != null ? true : false);
        model.addAttribute("queryIsEmpty", passengers.getTotalElements() == 0 ? true : false);

        return "passenger/list";
    }

    @GetMapping("/create")
    public String passengersCreateView(Passenger passenger, ModelMap model) {
        model.addAttribute("createView", true);
        return "passenger/createOrUpdate";
    }

    @PostMapping("/create/save")
    public String createPassenger(@Valid Passenger passenger, BindingResult result, ModelMap model, RedirectAttributes attr){

        if (result.hasErrors()) {
            model.addAttribute("createView", true);
            return "passenger/createOrUpdate";
        }

        passengerService.save(passenger);
        attr.addFlashAttribute("success", "<b>"+ passenger.getName() + "</b> adicionado com sucesso.");

        return "redirect:/passengers";
    }

    @GetMapping("/update/{rg}")
    public String travelUpdateView(@PathVariable("rg") String rg, ModelMap model, RedirectAttributes attr) {
        Passenger passenger = passengerService.findPassengerByRg(rg);
        User user = userService.getUserAuthenticated();

        if (!user.getStaff()) {
            if (!user.getHealthCenter().getId().equals(passenger.getHealthCenter().getId())) {
                attr.addFlashAttribute("error", "Você não tem permissões para editar este veículo.");
                return "redirect:/passengers";
            }
        }

        model.addAttribute("passenger", passenger);
        model.addAttribute("createView", false);
        return "passenger/createOrUpdate";
    }

    @PostMapping("/update/{rg}/save")
    public String passengerUpdateSave(@Valid Passenger passenger, BindingResult result, RedirectAttributes attr, ModelMap model) {

        if (result.hasErrors()) {
            model.addAttribute("createView", false);
            return "passenger/createOrUpdate";
        }

        passengerService.update(passenger);
        attr.addFlashAttribute("success", "<b>"+passenger.getName()+"</b> atualizado(a) com sucesso.");
        return "redirect:/passengers";
    }

    @GetMapping("/{rg}/deactivate")
    public String deactivatePassenger(@PathVariable("rg") String rg, RedirectAttributes attr) {
        Passenger passenger = passengerService.findPassengerByRg(rg);

        if(passenger != null && this.hasPermission(passenger.getHealthCenter().getId())){
            if (locationService.findLocationByPassengerAndTravelStatus(passenger.getId(), TravelStatus.AGUARDANDO.getName()).isEmpty()){
                passengerService.changePassengerStatus(passenger);
                attr.addFlashAttribute("success", "<b>"+passenger.getName()+"</b> desativado(a) com sucesso.");
            } else{
                attr.addFlashAttribute("error", "Este passageiro está em uma viagem ativa, tente novamente mais tarde.");
            }
        } else{
            attr.addFlashAttribute("error", "Erro ao tentar ativar o passageiro, tente novamente.");
        }

        return "redirect:/passengers";
    }

    @GetMapping("/{rg}/activate")
    public String activatePassenger(@PathVariable("rg") String rg, RedirectAttributes attr) {
        Passenger passenger = passengerService.findPassengerByRg(rg);

        if(this.hasPermission(passenger.getHealthCenter().getId()) && passenger != null){
            passengerService.changePassengerStatus(passenger);
            attr.addFlashAttribute("success", "<b>"+passenger.getName()+"</b> ativado(a) com sucesso.");
        } else{
            attr.addFlashAttribute("error", "Erro ao tentar ativar o passageiro, tente novamente.");
        }

        return "redirect:/passengers";
    }

    @GetMapping("/{rg}/delete")
    public String deletePassenger(@PathVariable("rg") String rg, RedirectAttributes attr){
        Passenger passenger = passengerService.findPassengerByRg(rg);
        String tempName = passenger.getShortName();

        if (passenger.canDelete()){
            passengerService.deletePassenger(passenger.getId());
            attr.addFlashAttribute("success", "<b>"+tempName+"</b> removido com sucesso.");
        } else {
            attr.addFlashAttribute("error", "<b>"+tempName+"</b> não pode ser removido(a)." +
                    " Este(a) passageiro(a) possuí locações atraladas a ele(a).");
        }

        return "redirect:/passengers";
    }

    @GetMapping("/{id}/change-status")
    public String changePassengerActive(@PathVariable("id") Long id, RedirectAttributes attr){
        Passenger passenger = passengerService.findById(id);
        passengerService.changePassengerStatus(passenger);
        return "redirect:/passengers";
    }

    @ModelAttribute("ufs")
    public UF[] getUFs() {
        return UF.values();
    }

    @ModelAttribute("genres")
    public Gender[] getGenres() {
        return Gender.values();
    }

    @ModelAttribute("healthCenters")
    public List<HealthCenter> healthCenters() {
        return healthCenterService.getModelAttribute();
    }

    public boolean hasPermission(Long idHealthCenter) {
        User user = userService.getUserAuthenticated();
        return (user.getStaff() || (user.getHealthCenter().getId().equals(idHealthCenter))) ? true : false;
    }
}
