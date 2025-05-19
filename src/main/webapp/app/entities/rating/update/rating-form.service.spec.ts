import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../rating.test-samples';

import { RatingFormService } from './rating-form.service';

describe('Rating Form Service', () => {
  let service: RatingFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RatingFormService);
  });

  describe('Service methods', () => {
    describe('createRatingFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createRatingFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            note: expect.any(Object),
            commentaire: expect.any(Object),
            ratingDate: expect.any(Object),
            trajet: expect.any(Object),
            passager: expect.any(Object),
            conducteur: expect.any(Object),
          }),
        );
      });

      it('passing IRating should create a new form with FormGroup', () => {
        const formGroup = service.createRatingFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            note: expect.any(Object),
            commentaire: expect.any(Object),
            ratingDate: expect.any(Object),
            trajet: expect.any(Object),
            passager: expect.any(Object),
            conducteur: expect.any(Object),
          }),
        );
      });
    });

    describe('getRating', () => {
      it('should return NewRating for default Rating initial value', () => {
        const formGroup = service.createRatingFormGroup(sampleWithNewData);

        const rating = service.getRating(formGroup) as any;

        expect(rating).toMatchObject(sampleWithNewData);
      });

      it('should return NewRating for empty Rating initial value', () => {
        const formGroup = service.createRatingFormGroup();

        const rating = service.getRating(formGroup) as any;

        expect(rating).toMatchObject({});
      });

      it('should return IRating', () => {
        const formGroup = service.createRatingFormGroup(sampleWithRequiredData);

        const rating = service.getRating(formGroup) as any;

        expect(rating).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IRating should not enable id FormControl', () => {
        const formGroup = service.createRatingFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewRating should disable id FormControl', () => {
        const formGroup = service.createRatingFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
