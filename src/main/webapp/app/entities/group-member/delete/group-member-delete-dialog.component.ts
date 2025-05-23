import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IGroupMember } from '../group-member.model';
import { GroupMemberService } from '../service/group-member.service';

@Component({
  templateUrl: './group-member-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class GroupMemberDeleteDialogComponent {
  groupMember?: IGroupMember;

  protected groupMemberService = inject(GroupMemberService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.groupMemberService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
