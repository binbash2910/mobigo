import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../ride.test-samples';

import { RideFormService } from './ride-form.service';

describe('Ride Form Service', () => {
  let service: RideFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RideFormService);
  });

  describe('Service methods', () => {
    describe('createRideFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createRideFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            villeDepart: expect.any(Object),
            villeArrivee: expect.any(Object),
            dateDepart: expect.any(Object),
            dateArrivee: expect.any(Object),
            heureDepart: expect.any(Object),
            heureArrivee: expect.any(Object),
            minuteDepart: expect.any(Object),
            minuteArrivee: expect.any(Object),
            prixParPlace: expect.any(Object),
            nbrePlaceDisponible: expect.any(Object),
            statut: expect.any(Object),
            vehicule: expect.any(Object),
          }),
        );
      });

      it('passing IRide should create a new form with FormGroup', () => {
        const formGroup = service.createRideFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            villeDepart: expect.any(Object),
            villeArrivee: expect.any(Object),
            dateDepart: expect.any(Object),
            dateArrivee: expect.any(Object),
            heureDepart: expect.any(Object),
            heureArrivee: expect.any(Object),
            minuteDepart: expect.any(Object),
            minuteArrivee: expect.any(Object),
            prixParPlace: expect.any(Object),
            nbrePlaceDisponible: expect.any(Object),
            statut: expect.any(Object),
            vehicule: expect.any(Object),
          }),
        );
      });
    });

    describe('getRide', () => {
      it('should return NewRide for default Ride initial value', () => {
        const formGroup = service.createRideFormGroup(sampleWithNewData);

        const ride = service.getRide(formGroup) as any;

        expect(ride).toMatchObject(sampleWithNewData);
      });

      it('should return NewRide for empty Ride initial value', () => {
        const formGroup = service.createRideFormGroup();

        const ride = service.getRide(formGroup) as any;

        expect(ride).toMatchObject({});
      });

      it('should return IRide', () => {
        const formGroup = service.createRideFormGroup(sampleWithRequiredData);

        const ride = service.getRide(formGroup) as any;

        expect(ride).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IRide should not enable id FormControl', () => {
        const formGroup = service.createRideFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewRide should disable id FormControl', () => {
        const formGroup = service.createRideFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
