import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IVehicle, NewVehicle } from '../vehicle.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IVehicle for edit and NewVehicleFormGroupInput for create.
 */
type VehicleFormGroupInput = IVehicle | PartialWithRequiredKeyOf<NewVehicle>;

type VehicleFormDefaults = Pick<NewVehicle, 'id'>;

type VehicleFormGroupContent = {
  id: FormControl<IVehicle['id'] | NewVehicle['id']>;
  marque: FormControl<IVehicle['marque']>;
  modele: FormControl<IVehicle['modele']>;
  annee: FormControl<IVehicle['annee']>;
  carteGrise: FormControl<IVehicle['carteGrise']>;
  immatriculation: FormControl<IVehicle['immatriculation']>;
  nbPlaces: FormControl<IVehicle['nbPlaces']>;
  couleur: FormControl<IVehicle['couleur']>;
  photo: FormControl<IVehicle['photo']>;
  actif: FormControl<IVehicle['actif']>;
  proprietaire: FormControl<IVehicle['proprietaire']>;
};

export type VehicleFormGroup = FormGroup<VehicleFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class VehicleFormService {
  createVehicleFormGroup(vehicle: VehicleFormGroupInput = { id: null }): VehicleFormGroup {
    const vehicleRawValue = {
      ...this.getFormDefaults(),
      ...vehicle,
    };
    return new FormGroup<VehicleFormGroupContent>({
      id: new FormControl(
        { value: vehicleRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      marque: new FormControl(vehicleRawValue.marque, {
        validators: [Validators.required],
      }),
      modele: new FormControl(vehicleRawValue.modele, {
        validators: [Validators.required],
      }),
      annee: new FormControl(vehicleRawValue.annee, {
        validators: [Validators.required],
      }),
      carteGrise: new FormControl(vehicleRawValue.carteGrise, {
        validators: [Validators.required],
      }),
      immatriculation: new FormControl(vehicleRawValue.immatriculation, {
        validators: [Validators.required],
      }),
      nbPlaces: new FormControl(vehicleRawValue.nbPlaces, {
        validators: [Validators.required],
      }),
      couleur: new FormControl(vehicleRawValue.couleur, {
        validators: [Validators.required],
      }),
      photo: new FormControl(vehicleRawValue.photo),
      actif: new FormControl(vehicleRawValue.actif, {
        validators: [Validators.required],
      }),
      proprietaire: new FormControl(vehicleRawValue.proprietaire),
    });
  }

  getVehicle(form: VehicleFormGroup): IVehicle | NewVehicle {
    return form.getRawValue() as IVehicle | NewVehicle;
  }

  resetForm(form: VehicleFormGroup, vehicle: VehicleFormGroupInput): void {
    const vehicleRawValue = { ...this.getFormDefaults(), ...vehicle };
    form.reset(
      {
        ...vehicleRawValue,
        id: { value: vehicleRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): VehicleFormDefaults {
    return {
      id: null,
    };
  }
}
