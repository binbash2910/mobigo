import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IGroupMember, NewGroupMember } from '../group-member.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IGroupMember for edit and NewGroupMemberFormGroupInput for create.
 */
type GroupMemberFormGroupInput = IGroupMember | PartialWithRequiredKeyOf<NewGroupMember>;

type GroupMemberFormDefaults = Pick<NewGroupMember, 'id'>;

type GroupMemberFormGroupContent = {
  id: FormControl<IGroupMember['id'] | NewGroupMember['id']>;
  group: FormControl<IGroupMember['group']>;
  user: FormControl<IGroupMember['user']>;
};

export type GroupMemberFormGroup = FormGroup<GroupMemberFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class GroupMemberFormService {
  createGroupMemberFormGroup(groupMember: GroupMemberFormGroupInput = { id: null }): GroupMemberFormGroup {
    const groupMemberRawValue = {
      ...this.getFormDefaults(),
      ...groupMember,
    };
    return new FormGroup<GroupMemberFormGroupContent>({
      id: new FormControl(
        { value: groupMemberRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      group: new FormControl(groupMemberRawValue.group),
      user: new FormControl(groupMemberRawValue.user),
    });
  }

  getGroupMember(form: GroupMemberFormGroup): IGroupMember | NewGroupMember {
    return form.getRawValue() as IGroupMember | NewGroupMember;
  }

  resetForm(form: GroupMemberFormGroup, groupMember: GroupMemberFormGroupInput): void {
    const groupMemberRawValue = { ...this.getFormDefaults(), ...groupMember };
    form.reset(
      {
        ...groupMemberRawValue,
        id: { value: groupMemberRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): GroupMemberFormDefaults {
    return {
      id: null,
    };
  }
}
