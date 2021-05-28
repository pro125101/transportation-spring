package com.pbd.project.service.driver;

import com.pbd.project.dao.driver.DriverDao;
import com.pbd.project.domain.Driver;
import com.pbd.project.domain.HealthCenter;
import com.pbd.project.domain.User;
import com.pbd.project.domain.Vehicle;
import com.pbd.project.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverDao driverDao;

    @Override
    public Driver save(Driver driver) {
        return driverDao.save(driver);
    }

    @Override
    public void delete(Long id) {
        driverDao.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Driver findDriverById(Long id) {
        return driverDao.findById(id).get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> getAllDrivers() {
        return driverDao.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> getAllDriversByAvailableAndActive(Long idHealthCenter, boolean isAvailable, boolean isActive) {
        return driverDao.findAvailables(idHealthCenter, isAvailable, isActive);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> getAllDriversByHealthCenter(HealthCenter healthCenter) {
        return driverDao.findDriverByHealthCenter(healthCenter);
    }

    @Override
    public void changeAvailable(Driver driver) {
        driver.setAvailable(driver.isAvailable() ? false : true);
        this.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Driver> getPaginatedDrivers(int currentPage) {
        return driverDao.findAll(getPageable(currentPage));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Driver> getDriversByHealthCenter(int currentPage, HealthCenter healthCenter) {
        return driverDao.findDriversByHealthCenter(getPageable(currentPage), healthCenter);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Driver> findDriversByNameAndHealthCenter(int currentPage, HealthCenter healthCenter, String name) {
        return driverDao.findDriversByHealthCenterAndNameContainsIgnoreCase(this.getPageable(currentPage), healthCenter, name);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Driver> findDriversByName(int currentPage, String name) {

        Page<Driver> drivers = driverDao.findDriversByNameContainsIgnoreCase(this.getPageable(currentPage), name);

        System.out.println(drivers);

        return drivers;
    }

    @Override
    public Page<Driver> getDrivers(int currentPage, String name, boolean isStaff, HealthCenter healthCenter) {
        if (name == null) {
            return isStaff ?
                    this.getPaginatedDrivers(currentPage) :
                    this.getDriversByHealthCenter(currentPage, healthCenter);
        } else {
            return isStaff ?
                    this.findDriversByName(currentPage, name) :
                    this.findDriversByNameAndHealthCenter(currentPage, healthCenter, name);
        }
    }

    public Pageable getPageable(int currentPage){
        return PageRequest.of(currentPage, 5, Sort.by("name").ascending());
    }



    
}
