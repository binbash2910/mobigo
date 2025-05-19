import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../step.test-samples';

import { StepFormService } from './step-form.service';

describe('Step Form Service', () => {
  let service: StepFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(StepFormService);
  });

  describe('Service methods', () => {
    describe('createStepFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createStepFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            ville: expect.any(Object),
            heureDepart: expect.any(Object),
            trajet: expect.any(Object),
          }),
        );
      });

      it('passing IStep should create a new form with FormGroup', () => {
        const formGroup = service.createStepFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            ville: expect.any(Object),
            heureDepart: expect.any(Object),
            trajet: expect.any(Object),
          }),
        );
      });
    });

    describe('getStep', () => {
      it('should return NewStep for default Step initial value', () => {
        const formGroup = service.createStepFormGroup(sampleWithNewData);

        const step = service.getStep(formGroup) as any;

        expect(step).toMatchObject(sampleWithNewData);
      });

      it('should return NewStep for empty Step initial value', () => {
        const formGroup = service.createStepFormGroup();

        const step = service.getStep(formGroup) as any;

        expect(step).toMatchObject({});
      });

      it('should return IStep', () => {
        const formGroup = service.createStepFormGroup(sampleWithRequiredData);

        const step = service.getStep(formGroup) as any;

        expect(step).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IStep should not enable id FormControl', () => {
        const formGroup = service.createStepFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewStep should disable id FormControl', () => {
        const formGroup = service.createStepFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
