import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IVehicle } from 'app/entities/vehicle/vehicle.model';
import { VehicleService } from 'app/entities/vehicle/service/vehicle.service';
import { RideService } from '../service/ride.service';
import { IRide } from '../ride.model';
import { RideFormService } from './ride-form.service';

import { RideUpdateComponent } from './ride-update.component';

describe('Ride Management Update Component', () => {
  let comp: RideUpdateComponent;
  let fixture: ComponentFixture<RideUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let rideFormService: RideFormService;
  let rideService: RideService;
  let vehicleService: VehicleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RideUpdateComponent],
      providers: [
        provideHttpClient(),
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(RideUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(RideUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    rideFormService = TestBed.inject(RideFormService);
    rideService = TestBed.inject(RideService);
    vehicleService = TestBed.inject(VehicleService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Vehicle query and add missing value', () => {
      const ride: IRide = { id: 30814 };
      const vehicule: IVehicle = { id: 18638 };
      ride.vehicule = vehicule;

      const vehicleCollection: IVehicle[] = [{ id: 18638 }];
      jest.spyOn(vehicleService, 'query').mockReturnValue(of(new HttpResponse({ body: vehicleCollection })));
      const additionalVehicles = [vehicule];
      const expectedCollection: IVehicle[] = [...additionalVehicles, ...vehicleCollection];
      jest.spyOn(vehicleService, 'addVehicleToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ ride });
      comp.ngOnInit();

      expect(vehicleService.query).toHaveBeenCalled();
      expect(vehicleService.addVehicleToCollectionIfMissing).toHaveBeenCalledWith(
        vehicleCollection,
        ...additionalVehicles.map(expect.objectContaining),
      );
      expect(comp.vehiclesSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const ride: IRide = { id: 30814 };
      const vehicule: IVehicle = { id: 18638 };
      ride.vehicule = vehicule;

      activatedRoute.data = of({ ride });
      comp.ngOnInit();

      expect(comp.vehiclesSharedCollection).toContainEqual(vehicule);
      expect(comp.ride).toEqual(ride);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IRide>>();
      const ride = { id: 7624 };
      jest.spyOn(rideFormService, 'getRide').mockReturnValue(ride);
      jest.spyOn(rideService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ ride });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: ride }));
      saveSubject.complete();

      // THEN
      expect(rideFormService.getRide).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(rideService.update).toHaveBeenCalledWith(expect.objectContaining(ride));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IRide>>();
      const ride = { id: 7624 };
      jest.spyOn(rideFormService, 'getRide').mockReturnValue({ id: null });
      jest.spyOn(rideService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ ride: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: ride }));
      saveSubject.complete();

      // THEN
      expect(rideFormService.getRide).toHaveBeenCalled();
      expect(rideService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IRide>>();
      const ride = { id: 7624 };
      jest.spyOn(rideService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ ride });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(rideService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareVehicle', () => {
      it('should forward to vehicleService', () => {
        const entity = { id: 18638 };
        const entity2 = { id: 22559 };
        jest.spyOn(vehicleService, 'compareVehicle');
        comp.compareVehicle(entity, entity2);
        expect(vehicleService.compareVehicle).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
