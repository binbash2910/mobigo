import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IAuthority } from 'app/entities/admin/authority/authority.model';
import { AuthorityService } from 'app/entities/admin/authority/service/authority.service';
import { IGroup } from 'app/entities/group/group.model';
import { GroupService } from 'app/entities/group/service/group.service';
import { GroupAuthorityService } from '../service/group-authority.service';
import { IGroupAuthority } from '../group-authority.model';
import { GroupAuthorityFormGroup, GroupAuthorityFormService } from './group-authority-form.service';

@Component({
  selector: 'jhi-group-authority-update',
  templateUrl: './group-authority-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class GroupAuthorityUpdateComponent implements OnInit {
  isSaving = false;
  groupAuthority: IGroupAuthority | null = null;

  authoritiesSharedCollection: IAuthority[] = [];
  groupsSharedCollection: IGroup[] = [];

  protected groupAuthorityService = inject(GroupAuthorityService);
  protected groupAuthorityFormService = inject(GroupAuthorityFormService);
  protected authorityService = inject(AuthorityService);
  protected groupService = inject(GroupService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GroupAuthorityFormGroup = this.groupAuthorityFormService.createGroupAuthorityFormGroup();

  compareAuthority = (o1: IAuthority | null, o2: IAuthority | null): boolean => this.authorityService.compareAuthority(o1, o2);

  compareGroup = (o1: IGroup | null, o2: IGroup | null): boolean => this.groupService.compareGroup(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ groupAuthority }) => {
      this.groupAuthority = groupAuthority;
      if (groupAuthority) {
        this.updateForm(groupAuthority);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const groupAuthority = this.groupAuthorityFormService.getGroupAuthority(this.editForm);
    if (groupAuthority.id !== null) {
      this.subscribeToSaveResponse(this.groupAuthorityService.update(groupAuthority));
    } else {
      this.subscribeToSaveResponse(this.groupAuthorityService.create(groupAuthority));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupAuthority>>): void {
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

  protected updateForm(groupAuthority: IGroupAuthority): void {
    this.groupAuthority = groupAuthority;
    this.groupAuthorityFormService.resetForm(this.editForm, groupAuthority);

    this.authoritiesSharedCollection = this.authorityService.addAuthorityToCollectionIfMissing<IAuthority>(
      this.authoritiesSharedCollection,
      groupAuthority.authority,
    );
    this.groupsSharedCollection = this.groupService.addGroupToCollectionIfMissing<IGroup>(
      this.groupsSharedCollection,
      groupAuthority.group,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.authorityService
      .query()
      .pipe(map((res: HttpResponse<IAuthority[]>) => res.body ?? []))
      .pipe(
        map((authorities: IAuthority[]) =>
          this.authorityService.addAuthorityToCollectionIfMissing<IAuthority>(authorities, this.groupAuthority?.authority),
        ),
      )
      .subscribe((authorities: IAuthority[]) => (this.authoritiesSharedCollection = authorities));

    this.groupService
      .query()
      .pipe(map((res: HttpResponse<IGroup[]>) => res.body ?? []))
      .pipe(map((groups: IGroup[]) => this.groupService.addGroupToCollectionIfMissing<IGroup>(groups, this.groupAuthority?.group)))
      .subscribe((groups: IGroup[]) => (this.groupsSharedCollection = groups));
  }
}
