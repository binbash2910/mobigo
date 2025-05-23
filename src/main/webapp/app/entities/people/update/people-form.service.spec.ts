import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../people.test-samples';

import { PeopleFormService } from './people-form.service';

describe('People Form Service', () => {
  let service: PeopleFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PeopleFormService);
  });

  describe('Service methods', () => {
    describe('createPeopleFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createPeopleFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            nom: expect.any(Object),
            prenom: expect.any(Object),
            telephone: expect.any(Object),
            cni: expect.any(Object),
            photo: expect.any(Object),
            actif: expect.any(Object),
            dateNaissance: expect.any(Object),
            musique: expect.any(Object),
            discussion: expect.any(Object),
            cigarette: expect.any(Object),
            alcool: expect.any(Object),
            animaux: expect.any(Object),
            conducteur: expect.any(Object),
            passager: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });

      it('passing IPeople should create a new form with FormGroup', () => {
        const formGroup = service.createPeopleFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            nom: expect.any(Object),
            prenom: expect.any(Object),
            telephone: expect.any(Object),
            cni: expect.any(Object),
            photo: expect.any(Object),
            actif: expect.any(Object),
            dateNaissance: expect.any(Object),
            musique: expect.any(Object),
            discussion: expect.any(Object),
            cigarette: expect.any(Object),
            alcool: expect.any(Object),
            animaux: expect.any(Object),
            conducteur: expect.any(Object),
            passager: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });
    });

    describe('getPeople', () => {
      it('should return NewPeople for default People initial value', () => {
        const formGroup = service.createPeopleFormGroup(sampleWithNewData);

        const people = service.getPeople(formGroup) as any;

        expect(people).toMatchObject(sampleWithNewData);
      });

      it('should return NewPeople for empty People initial value', () => {
        const formGroup = service.createPeopleFormGroup();

        const people = service.getPeople(formGroup) as any;

        expect(people).toMatchObject({});
      });

      it('should return IPeople', () => {
        const formGroup = service.createPeopleFormGroup(sampleWithRequiredData);

        const people = service.getPeople(formGroup) as any;

        expect(people).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IPeople should not enable id FormControl', () => {
        const formGroup = service.createPeopleFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewPeople should disable id FormControl', () => {
        const formGroup = service.createPeopleFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
