import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IStep, NewStep } from '../step.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IStep for edit and NewStepFormGroupInput for create.
 */
type StepFormGroupInput = IStep | PartialWithRequiredKeyOf<NewStep>;

type StepFormDefaults = Pick<NewStep, 'id'>;

type StepFormGroupContent = {
  id: FormControl<IStep['id'] | NewStep['id']>;
  ville: FormControl<IStep['ville']>;
  heureDepart: FormControl<IStep['heureDepart']>;
  trajet: FormControl<IStep['trajet']>;
};

export type StepFormGroup = FormGroup<StepFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class StepFormService {
  createStepFormGroup(step: StepFormGroupInput = { id: null }): StepFormGroup {
    const stepRawValue = {
      ...this.getFormDefaults(),
      ...step,
    };
    return new FormGroup<StepFormGroupContent>({
      id: new FormControl(
        { value: stepRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      ville: new FormControl(stepRawValue.ville, {
        validators: [Validators.required],
      }),
      heureDepart: new FormControl(stepRawValue.heureDepart, {
        validators: [Validators.required],
      }),
      trajet: new FormControl(stepRawValue.trajet),
    });
  }

  getStep(form: StepFormGroup): IStep | NewStep {
    return form.getRawValue() as IStep | NewStep;
  }

  resetForm(form: StepFormGroup, step: StepFormGroupInput): void {
    const stepRawValue = { ...this.getFormDefaults(), ...step };
    form.reset(
      {
        ...stepRawValue,
        id: { value: stepRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): StepFormDefaults {
    return {
      id: null,
    };
  }
}
