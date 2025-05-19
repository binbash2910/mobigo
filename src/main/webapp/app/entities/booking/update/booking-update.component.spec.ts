import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IRide } from 'app/entities/ride/ride.model';
import { RideService } from 'app/entities/ride/service/ride.service';
import { IPeople } from 'app/entities/people/people.model';
import { PeopleService } from 'app/entities/people/service/people.service';
import { IBooking } from '../booking.model';
import { BookingService } from '../service/booking.service';
import { BookingFormService } from './booking-form.service';

import { BookingUpdateComponent } from './booking-update.component';

describe('Booking Management Update Component', () => {
  let comp: BookingUpdateComponent;
  let fixture: ComponentFixture<BookingUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let bookingFormService: BookingFormService;
  let bookingService: BookingService;
  let rideService: RideService;
  let peopleService: PeopleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BookingUpdateComponent],
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
      .overrideTemplate(BookingUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(BookingUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    bookingFormService = TestBed.inject(BookingFormService);
    bookingService = TestBed.inject(BookingService);
    rideService = TestBed.inject(RideService);
    peopleService = TestBed.inject(PeopleService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Ride query and add missing value', () => {
      const booking: IBooking = { id: 4697 };
      const trajet: IRide = { id: 7624 };
      booking.trajet = trajet;

      const rideCollection: IRide[] = [{ id: 7624 }];
      jest.spyOn(rideService, 'query').mockReturnValue(of(new HttpResponse({ body: rideCollection })));
      const additionalRides = [trajet];
      const expectedCollection: IRide[] = [...additionalRides, ...rideCollection];
      jest.spyOn(rideService, 'addRideToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ booking });
      comp.ngOnInit();

      expect(rideService.query).toHaveBeenCalled();
      expect(rideService.addRideToCollectionIfMissing).toHaveBeenCalledWith(
        rideCollection,
        ...additionalRides.map(expect.objectContaining),
      );
      expect(comp.ridesSharedCollection).toEqual(expectedCollection);
    });

    it('should call People query and add missing value', () => {
      const booking: IBooking = { id: 4697 };
      const passager: IPeople = { id: 9353 };
      booking.passager = passager;

      const peopleCollection: IPeople[] = [{ id: 9353 }];
      jest.spyOn(peopleService, 'query').mockReturnValue(of(new HttpResponse({ body: peopleCollection })));
      const additionalPeople = [passager];
      const expectedCollection: IPeople[] = [...additionalPeople, ...peopleCollection];
      jest.spyOn(peopleService, 'addPeopleToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ booking });
      comp.ngOnInit();

      expect(peopleService.query).toHaveBeenCalled();
      expect(peopleService.addPeopleToCollectionIfMissing).toHaveBeenCalledWith(
        peopleCollection,
        ...additionalPeople.map(expect.objectContaining),
      );
      expect(comp.peopleSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const booking: IBooking = { id: 4697 };
      const trajet: IRide = { id: 7624 };
      booking.trajet = trajet;
      const passager: IPeople = { id: 9353 };
      booking.passager = passager;

      activatedRoute.data = of({ booking });
      comp.ngOnInit();

      expect(comp.ridesSharedCollection).toContainEqual(trajet);
      expect(comp.peopleSharedCollection).toContainEqual(passager);
      expect(comp.booking).toEqual(booking);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBooking>>();
      const booking = { id: 1408 };
      jest.spyOn(bookingFormService, 'getBooking').mockReturnValue(booking);
      jest.spyOn(bookingService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ booking });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: booking }));
      saveSubject.complete();

      // THEN
      expect(bookingFormService.getBooking).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(bookingService.update).toHaveBeenCalledWith(expect.objectContaining(booking));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBooking>>();
      const booking = { id: 1408 };
      jest.spyOn(bookingFormService, 'getBooking').mockReturnValue({ id: null });
      jest.spyOn(bookingService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ booking: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: booking }));
      saveSubject.complete();

      // THEN
      expect(bookingFormService.getBooking).toHaveBeenCalled();
      expect(bookingService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBooking>>();
      const booking = { id: 1408 };
      jest.spyOn(bookingService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ booking });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(bookingService.update).toHaveBeenCalled();
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
