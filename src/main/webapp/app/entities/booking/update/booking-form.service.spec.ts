import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../booking.test-samples';

import { BookingFormService } from './booking-form.service';

describe('Booking Form Service', () => {
  let service: BookingFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BookingFormService);
  });

  describe('Service methods', () => {
    describe('createBookingFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createBookingFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            nbPlacesReservees: expect.any(Object),
            montantTotal: expect.any(Object),
            dateReservation: expect.any(Object),
            statut: expect.any(Object),
            trajet: expect.any(Object),
            passager: expect.any(Object),
          }),
        );
      });

      it('passing IBooking should create a new form with FormGroup', () => {
        const formGroup = service.createBookingFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            nbPlacesReservees: expect.any(Object),
            montantTotal: expect.any(Object),
            dateReservation: expect.any(Object),
            statut: expect.any(Object),
            trajet: expect.any(Object),
            passager: expect.any(Object),
          }),
        );
      });
    });

    describe('getBooking', () => {
      it('should return NewBooking for default Booking initial value', () => {
        const formGroup = service.createBookingFormGroup(sampleWithNewData);

        const booking = service.getBooking(formGroup) as any;

        expect(booking).toMatchObject(sampleWithNewData);
      });

      it('should return NewBooking for empty Booking initial value', () => {
        const formGroup = service.createBookingFormGroup();

        const booking = service.getBooking(formGroup) as any;

        expect(booking).toMatchObject({});
      });

      it('should return IBooking', () => {
        const formGroup = service.createBookingFormGroup(sampleWithRequiredData);

        const booking = service.getBooking(formGroup) as any;

        expect(booking).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IBooking should not enable id FormControl', () => {
        const formGroup = service.createBookingFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewBooking should disable id FormControl', () => {
        const formGroup = service.createBookingFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
