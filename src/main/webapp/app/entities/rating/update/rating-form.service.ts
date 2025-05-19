import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IRating, NewRating } from '../rating.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IRating for edit and NewRatingFormGroupInput for create.
 */
type RatingFormGroupInput = IRating | PartialWithRequiredKeyOf<NewRating>;

type RatingFormDefaults = Pick<NewRating, 'id'>;

type RatingFormGroupContent = {
  id: FormControl<IRating['id'] | NewRating['id']>;
  note: FormControl<IRating['note']>;
  commentaire: FormControl<IRating['commentaire']>;
  ratingDate: FormControl<IRating['ratingDate']>;
  trajet: FormControl<IRating['trajet']>;
  passager: FormControl<IRating['passager']>;
  conducteur: FormControl<IRating['conducteur']>;
};

export type RatingFormGroup = FormGroup<RatingFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class RatingFormService {
  createRatingFormGroup(rating: RatingFormGroupInput = { id: null }): RatingFormGroup {
    const ratingRawValue = {
      ...this.getFormDefaults(),
      ...rating,
    };
    return new FormGroup<RatingFormGroupContent>({
      id: new FormControl(
        { value: ratingRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      note: new FormControl(ratingRawValue.note),
      commentaire: new FormControl(ratingRawValue.commentaire),
      ratingDate: new FormControl(ratingRawValue.ratingDate, {
        validators: [Validators.required],
      }),
      trajet: new FormControl(ratingRawValue.trajet),
      passager: new FormControl(ratingRawValue.passager),
      conducteur: new FormControl(ratingRawValue.conducteur),
    });
  }

  getRating(form: RatingFormGroup): IRating | NewRating {
    return form.getRawValue() as IRating | NewRating;
  }

  resetForm(form: RatingFormGroup, rating: RatingFormGroupInput): void {
    const ratingRawValue = { ...this.getFormDefaults(), ...rating };
    form.reset(
      {
        ...ratingRawValue,
        id: { value: ratingRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): RatingFormDefaults {
    return {
      id: null,
    };
  }
}
