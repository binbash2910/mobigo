import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IPeople, NewPeople } from '../people.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IPeople for edit and NewPeopleFormGroupInput for create.
 */
type PeopleFormGroupInput = IPeople | PartialWithRequiredKeyOf<NewPeople>;

type PeopleFormDefaults = Pick<NewPeople, 'id'>;

type PeopleFormGroupContent = {
  id: FormControl<IPeople['id'] | NewPeople['id']>;
  nom: FormControl<IPeople['nom']>;
  prenom: FormControl<IPeople['prenom']>;
  telephone: FormControl<IPeople['telephone']>;
  cni: FormControl<IPeople['cni']>;
  photo: FormControl<IPeople['photo']>;
  actif: FormControl<IPeople['actif']>;
  dateNaissance: FormControl<IPeople['dateNaissance']>;
  musique: FormControl<IPeople['musique']>;
  discussion: FormControl<IPeople['discussion']>;
  cigarette: FormControl<IPeople['cigarette']>;
  alcool: FormControl<IPeople['alcool']>;
  animaux: FormControl<IPeople['animaux']>;
  conducteur: FormControl<IPeople['conducteur']>;
  passager: FormControl<IPeople['passager']>;
  user: FormControl<IPeople['user']>;
};

export type PeopleFormGroup = FormGroup<PeopleFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class PeopleFormService {
  createPeopleFormGroup(people: PeopleFormGroupInput = { id: null }): PeopleFormGroup {
    const peopleRawValue = {
      ...this.getFormDefaults(),
      ...people,
    };
    return new FormGroup<PeopleFormGroupContent>({
      id: new FormControl(
        { value: peopleRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      nom: new FormControl(peopleRawValue.nom, {
        validators: [Validators.required],
      }),
      prenom: new FormControl(peopleRawValue.prenom),
      telephone: new FormControl(peopleRawValue.telephone, {
        validators: [Validators.required],
      }),
      cni: new FormControl(peopleRawValue.cni, {
        validators: [Validators.required],
      }),
      photo: new FormControl(peopleRawValue.photo),
      actif: new FormControl(peopleRawValue.actif, {
        validators: [Validators.required],
      }),
      dateNaissance: new FormControl(peopleRawValue.dateNaissance, {
        validators: [Validators.required],
      }),
      musique: new FormControl(peopleRawValue.musique),
      discussion: new FormControl(peopleRawValue.discussion),
      cigarette: new FormControl(peopleRawValue.cigarette),
      alcool: new FormControl(peopleRawValue.alcool),
      animaux: new FormControl(peopleRawValue.animaux),
      conducteur: new FormControl(peopleRawValue.conducteur),
      passager: new FormControl(peopleRawValue.passager),
      user: new FormControl(peopleRawValue.user),
    });
  }

  getPeople(form: PeopleFormGroup): IPeople | NewPeople {
    return form.getRawValue() as IPeople | NewPeople;
  }

  resetForm(form: PeopleFormGroup, people: PeopleFormGroupInput): void {
    const peopleRawValue = { ...this.getFormDefaults(), ...people };
    form.reset(
      {
        ...peopleRawValue,
        id: { value: peopleRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): PeopleFormDefaults {
    return {
      id: null,
    };
  }
}
