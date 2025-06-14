import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IGroupMember } from '../group-member.model';

@Component({
  selector: 'jhi-group-member-detail',
  templateUrl: './group-member-detail.component.html',
  imports: [SharedModule, RouterModule],
})
export class GroupMemberDetailComponent {
  groupMember = input<IGroupMember | null>(null);

  previousState(): void {
    window.history.back();
  }
}
