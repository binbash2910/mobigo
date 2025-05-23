import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IGroup } from 'app/entities/group/group.model';
import { GroupService } from 'app/entities/group/service/group.service';
import { IUser } from 'app/entities/user/user.model';
import { UserService } from 'app/entities/user/service/user.service';
import { GroupMemberService } from '../service/group-member.service';
import { IGroupMember } from '../group-member.model';
import { GroupMemberFormGroup, GroupMemberFormService } from './group-member-form.service';

@Component({
  selector: 'jhi-group-member-update',
  templateUrl: './group-member-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class GroupMemberUpdateComponent implements OnInit {
  isSaving = false;
  groupMember: IGroupMember | null = null;

  groupsSharedCollection: IGroup[] = [];
  usersSharedCollection: IUser[] = [];

  protected groupMemberService = inject(GroupMemberService);
  protected groupMemberFormService = inject(GroupMemberFormService);
  protected groupService = inject(GroupService);
  protected userService = inject(UserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GroupMemberFormGroup = this.groupMemberFormService.createGroupMemberFormGroup();

  compareGroup = (o1: IGroup | null, o2: IGroup | null): boolean => this.groupService.compareGroup(o1, o2);

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ groupMember }) => {
      this.groupMember = groupMember;
      if (groupMember) {
        this.updateForm(groupMember);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const groupMember = this.groupMemberFormService.getGroupMember(this.editForm);
    if (groupMember.id !== null) {
      this.subscribeToSaveResponse(this.groupMemberService.update(groupMember));
    } else {
      this.subscribeToSaveResponse(this.groupMemberService.create(groupMember));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupMember>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(groupMember: IGroupMember): void {
    this.groupMember = groupMember;
    this.groupMemberFormService.resetForm(this.editForm, groupMember);

    this.groupsSharedCollection = this.groupService.addGroupToCollectionIfMissing<IGroup>(this.groupsSharedCollection, groupMember.group);
    this.usersSharedCollection = this.userService.addUserToCollectionIfMissing<IUser>(this.usersSharedCollection, groupMember.user);
  }

  protected loadRelationshipsOptions(): void {
    this.groupService
      .query()
      .pipe(map((res: HttpResponse<IGroup[]>) => res.body ?? []))
      .pipe(map((groups: IGroup[]) => this.groupService.addGroupToCollectionIfMissing<IGroup>(groups, this.groupMember?.group)))
      .subscribe((groups: IGroup[]) => (this.groupsSharedCollection = groups));

    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.groupMember?.user)))
      .subscribe((users: IUser[]) => (this.usersSharedCollection = users));
  }
}
