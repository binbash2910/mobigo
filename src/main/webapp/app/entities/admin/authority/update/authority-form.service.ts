import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IAuthority, NewAuthority } from '../authority.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { name: unknown }> = Partial<Omit<T, 'name'>> & { name: T['name'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IAuthority for edit and NewAuthorityFormGroupInput for create.
 */
type AuthorityFormGroupInput = IAuthority | PartialWithRequiredKeyOf<NewAuthority>;

type AuthorityFormDefaults = Pick<NewAuthority, 'name'>;

type AuthorityFormGroupContent = {
  name: FormControl<IAuthority['name'] | NewAuthority['name']>;
  description: FormControl<IAuthority['description']>;
  ordre: FormControl<IAuthority['ordre']>;
};

export type AuthorityFormGroup = FormGroup<AuthorityFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class AuthorityFormService {
  createAuthorityFormGroup(authority: AuthorityFormGroupInput = { name: null }): AuthorityFormGroup {
    const authorityRawValue = {
      ...this.getFormDefaults(),
      ...authority,
    };
    return new FormGroup<AuthorityFormGroupContent>({
      name: new FormControl(
        { value: authorityRawValue.name, disabled: authorityRawValue.name !== null },
        {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(50)],
        },
      ),
      description: new FormControl(authorityRawValue.description),
      ordre: new FormControl(authorityRawValue.ordre),
    });
  }

  getAuthority(form: AuthorityFormGroup): IAuthority | NewAuthority {
    return form.getRawValue() as IAuthority | NewAuthority;
  }

  resetForm(form: AuthorityFormGroup, authority: AuthorityFormGroupInput): void {
    const authorityRawValue = { ...this.getFormDefaults(), ...authority };
    form.reset(
      {
        ...authorityRawValue,
        name: { value: authorityRawValue.name, disabled: authorityRawValue.name !== null },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): AuthorityFormDefaults {
    return {
      name: null,
    };
  }
}
