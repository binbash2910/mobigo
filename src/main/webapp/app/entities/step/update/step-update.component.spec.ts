import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IRide } from 'app/entities/ride/ride.model';
import { RideService } from 'app/entities/ride/service/ride.service';
import { StepService } from '../service/step.service';
import { IStep } from '../step.model';
import { StepFormService } from './step-form.service';

import { StepUpdateComponent } from './step-update.component';

describe('Step Management Update Component', () => {
  let comp: StepUpdateComponent;
  let fixture: ComponentFixture<StepUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let stepFormService: StepFormService;
  let stepService: StepService;
  let rideService: RideService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [StepUpdateComponent],
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
      .overrideTemplate(StepUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(StepUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    stepFormService = TestBed.inject(StepFormService);
    stepService = TestBed.inject(StepService);
    rideService = TestBed.inject(RideService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Ride query and add missing value', () => {
      const step: IStep = { id: 154 };
      const trajet: IRide = { id: 7624 };
      step.trajet = trajet;

      const rideCollection: IRide[] = [{ id: 7624 }];
      jest.spyOn(rideService, 'query').mockReturnValue(of(new HttpResponse({ body: rideCollection })));
      const additionalRides = [trajet];
      const expectedCollection: IRide[] = [...additionalRides, ...rideCollection];
      jest.spyOn(rideService, 'addRideToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ step });
      comp.ngOnInit();

      expect(rideService.query).toHaveBeenCalled();
      expect(rideService.addRideToCollectionIfMissing).toHaveBeenCalledWith(
        rideCollection,
        ...additionalRides.map(expect.objectContaining),
      );
      expect(comp.ridesSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const step: IStep = { id: 154 };
      const trajet: IRide = { id: 7624 };
      step.trajet = trajet;

      activatedRoute.data = of({ step });
      comp.ngOnInit();

      expect(comp.ridesSharedCollection).toContainEqual(trajet);
      expect(comp.step).toEqual(step);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IStep>>();
      const step = { id: 20214 };
      jest.spyOn(stepFormService, 'getStep').mockReturnValue(step);
      jest.spyOn(stepService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ step });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: step }));
      saveSubject.complete();

      // THEN
      expect(stepFormService.getStep).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(stepService.update).toHaveBeenCalledWith(expect.objectContaining(step));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IStep>>();
      const step = { id: 20214 };
      jest.spyOn(stepFormService, 'getStep').mockReturnValue({ id: null });
      jest.spyOn(stepService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ step: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: step }));
      saveSubject.complete();

      // THEN
      expect(stepFormService.getStep).toHaveBeenCalled();
      expect(stepService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IStep>>();
      const step = { id: 20214 };
      jest.spyOn(stepService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ step });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(stepService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareRide', () => {
      it('should forward to rideService', () => {
        const entity = { id: 7624 };
        const entity2 = { id: 30814 };
        jest.spyOn(rideService, 'compareRide');
        comp.compareRide(entity, entity2);
        expect(rideService.compareRide).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
