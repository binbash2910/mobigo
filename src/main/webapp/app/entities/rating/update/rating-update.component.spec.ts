import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IRide } from 'app/entities/ride/ride.model';
import { RideService } from 'app/entities/ride/service/ride.service';
import { IPeople } from 'app/entities/people/people.model';
import { PeopleService } from 'app/entities/people/service/people.service';
import { IRating } from '../rating.model';
import { RatingService } from '../service/rating.service';
import { RatingFormService } from './rating-form.service';

import { RatingUpdateComponent } from './rating-update.component';

describe('Rating Management Update Component', () => {
  let comp: RatingUpdateComponent;
  let fixture: ComponentFixture<RatingUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let ratingFormService: RatingFormService;
  let ratingService: RatingService;
  let rideService: RideService;
  let peopleService: PeopleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RatingUpdateComponent],
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
      .overrideTemplate(RatingUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(RatingUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    ratingFormService = TestBed.inject(RatingFormService);
    ratingService = TestBed.inject(RatingService);
    rideService = TestBed.inject(RideService);
    peopleService = TestBed.inject(PeopleService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Ride query and add missing value', () => {
      const rating: IRating = { id: 11888 };
      const trajet: IRide = { id: 7624 };
      rating.trajet = trajet;

      const rideCollection: IRide[] = [{ id: 7624 }];
      jest.spyOn(rideService, 'query').mockReturnValue(of(new HttpResponse({ body: rideCollection })));
      const additionalRides = [trajet];
      const expectedCollection: IRide[] = [...additionalRides, ...rideCollection];
      jest.spyOn(rideService, 'addRideToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ rating });
      comp.ngOnInit();

      expect(rideService.query).toHaveBeenCalled();
      expect(rideService.addRideToCollectionIfMissing).toHaveBeenCalledWith(
        rideCollection,
        ...additionalRides.map(expect.objectContaining),
      );
      expect(comp.ridesSharedCollection).toEqual(expectedCollection);
    });

    it('should call People query and add missing value', () => {
      const rating: IRating = { id: 11888 };
      const passager: IPeople = { id: 9353 };
      rating.passager = passager;
      const conducteur: IPeople = { id: 9353 };
      rating.conducteur = conducteur;

      const peopleCollection: IPeople[] = [{ id: 9353 }];
      jest.spyOn(peopleService, 'query').mockReturnValue(of(new HttpResponse({ body: peopleCollection })));
      const additionalPeople = [passager, conducteur];
      const expectedCollection: IPeople[] = [...additionalPeople, ...peopleCollection];
      jest.spyOn(peopleService, 'addPeopleToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ rating });
      comp.ngOnInit();

      expect(peopleService.query).toHaveBeenCalled();
      expect(peopleService.addPeopleToCollectionIfMissing).toHaveBeenCalledWith(
        peopleCollection,
        ...additionalPeople.map(expect.objectContaining),
      );
      expect(comp.peopleSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const rating: IRating = { id: 11888 };
      const trajet: IRide = { id: 7624 };
      rating.trajet = trajet;
      const passager: IPeople = { id: 9353 };
      rating.passager = passager;
      const conducteur: IPeople = { id: 9353 };
      rating.conducteur = conducteur;

      activatedRoute.data = of({ rating });
      comp.ngOnInit();

      expect(comp.ridesSharedCollection).toContainEqual(trajet);
      expect(comp.peopleSharedCollection).toContainEqual(passager);
      expect(comp.peopleSharedCollection).toContainEqual(conducteur);
      expect(comp.rating).toEqual(rating);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IRating>>();
      const rating = { id: 11381 };
      jest.spyOn(ratingFormService, 'getRating').mockReturnValue(rating);
      jest.spyOn(ratingService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ rating });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: rating }));
      saveSubject.complete();

      // THEN
      expect(ratingFormService.getRating).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(ratingService.update).toHaveBeenCalledWith(expect.objectContaining(rating));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IRating>>();
      const rating = { id: 11381 };
      jest.spyOn(ratingFormService, 'getRating').mockReturnValue({ id: null });
      jest.spyOn(ratingService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ rating: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: rating }));
      saveSubject.complete();

      // THEN
      expect(ratingFormService.getRating).toHaveBeenCalled();
      expect(ratingService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IRating>>();
      const rating = { id: 11381 };
      jest.spyOn(ratingService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ rating });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(ratingService.update).toHaveBeenCalled();
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

    describe('comparePeople', () => {
      it('should forward to peopleService', () => {
        const entity = { id: 9353 };
        const entity2 = { id: 20275 };
        jest.spyOn(peopleService, 'comparePeople');
        comp.comparePeople(entity, entity2);
        expect(peopleService.comparePeople).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
