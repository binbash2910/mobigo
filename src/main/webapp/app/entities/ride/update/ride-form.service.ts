import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IRide, NewRide } from '../ride.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IRide for edit and NewRideFormGroupInput for create.
 */
type RideFormGroupInput = IRide | PartialWithRequiredKeyOf<NewRide>;

type RideFormDefaults = Pick<NewRide, 'id'>;

type RideFormGroupContent = {
  id: FormControl<IRide['id'] | NewRide['id']>;
  villeDepart: FormControl<IRide['villeDepart']>;
  villeArrivee: FormControl<IRide['villeArrivee']>;
  dateDepart: FormControl<IRide['dateDepart']>;
  dateArrivee: FormControl<IRide['dateArrivee']>;
  heureDepart: FormControl<IRide['heureDepart']>;
  heureArrivee: FormControl<IRide['heureArrivee']>;
  minuteDepart: FormControl<IRide['minuteDepart']>;
  minuteArrivee: FormControl<IRide['minuteArrivee']>;
  prixParPlace: FormControl<IRide['prixParPlace']>;
  nbrePlaceDisponible: FormControl<IRide['nbrePlaceDisponible']>;
  statut: FormControl<IRide['statut']>;
  vehicule: FormControl<IRide['vehicule']>;
};

export type RideFormGroup = FormGroup<RideFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class RideFormService {
  createRideFormGroup(ride: RideFormGroupInput = { id: null }): RideFormGroup {
    const rideRawValue = {
      ...this.getFormDefaults(),
      ...ride,
    };
    return new FormGroup<RideFormGroupContent>({
      id: new FormControl(
        { value: rideRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      villeDepart: new FormControl(rideRawValue.villeDepart, {
        validators: [Validators.required],
      }),
      villeArrivee: new FormControl(rideRawValue.villeArrivee, {
        validators: [Validators.required],
      }),
      dateDepart: new FormControl(rideRawValue.dateDepart, {
        validators: [Validators.required],
      }),
      dateArrivee: new FormControl(rideRawValue.dateArrivee, {
        validators: [Validators.required],
      }),
      heureDepart: new FormControl(rideRawValue.heureDepart, {
        validators: [Validators.required],
      }),
      heureArrivee: new FormControl(rideRawValue.heureArrivee, {
        validators: [Validators.required],
      }),
      minuteDepart: new FormControl(rideRawValue.minuteDepart, {
        validators: [Validators.required],
      }),
      minuteArrivee: new FormControl(rideRawValue.minuteArrivee, {
        validators: [Validators.required],
      }),
      prixParPlace: new FormControl(rideRawValue.prixParPlace, {
        validators: [Validators.required],
      }),
      nbrePlaceDisponible: new FormControl(rideRawValue.nbrePlaceDisponible, {
        validators: [Validators.required],
      }),
      statut: new FormControl(rideRawValue.statut, {
        validators: [Validators.required],
      }),
      vehicule: new FormControl(rideRawValue.vehicule),
    });
  }

  getRide(form: RideFormGroup): IRide | NewRide {
    return form.getRawValue() as IRide | NewRide;
  }

  resetForm(form: RideFormGroup, ride: RideFormGroupInput): void {
    const rideRawValue = { ...this.getFormDefaults(), ...ride };
    form.reset(
      {
        ...rideRawValue,
        id: { value: rideRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): RideFormDefaults {
    return {
      id: null,
    };
  }
}
